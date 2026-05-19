package com.naeir.bt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;
import com.github.cdefgah.bencoder4j.model.BencodedObject;
import com.naeir.bt.TorrentMeta.MetaFile;
import com.naelir.dht.BDecoder;
import com.naelir.dht.From;

import io.netty.buffer.ByteBuf;

class RawTorrentMetadata {
    public static final long METADATA_PIECE_SIZE = 16 << 10;
    public static final byte[] PIECES = new byte[] { 54, 58, 112, 105, 101, 99, 101, 115 };
    public static final byte[] EE = new byte[] { 101, 101 };
    public static final Logger logger = LogManager.getLogger(RawTorrentMetadata.class);

    private static int indexOf(byte[] parent, byte[] child, int position) {
        for (int i = position; i < parent.length - child.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < child.length; ++j) {
                if (parent[i + j] != child[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;
    }

    private static Optional<BencodedDictionary> parse(byte[] array) {
        int indexOf = indexOf(array, PIECES, 0);
        int i = indexOf + 1 - 2;
        byte[] important = new byte[i];
        System.arraycopy(array, 2, important, 0, i - 1);
        important[i - 1] = 101;
        int skip = indexOf(important, EE, 0) + 2;
        int filesInfoSize = important.length - skip;
        byte[] files = new byte[filesInfoSize];
        System.arraycopy(important, skip, files, 0, filesInfoSize);
        return BDecoder.decode(files);
    }

    ByteBuffer bytes;
    boolean firstPacket = true;
    private ByteBuffer torrent;

    public RawTorrentMetadata(ByteBuffer torrent) {
        this.torrent = torrent;
    }

    public void read(ByteBuf buffer) {
        if (this.firstPacket) {
            int length = buffer.readInt();
            this.bytes = ByteBuffer.allocate(length);
            this.firstPacket = false;
        }
        int readableBytes = buffer.readableBytes();
        byte[] data = new byte[readableBytes];
        buffer.readBytes(data);
        this.bytes.put(data, 0, data.length);
    }

    public Optional<TorrentMeta> resolve(From from) {
        Optional<BencodedDictionary> optional = parse(this.bytes.array());
        if (optional.isPresent()) {
            try {
                BencodedDictionary map = optional.get();
                List<MetaFile> list = new ArrayList<>();
                BencodedList files = (BencodedList) map.get(BtKeys.FILES);
                for (BencodedObject e : files) {
                    BencodedDictionary file = (BencodedDictionary) e;
                    BencodedInteger length = (BencodedInteger) file.get(BtKeys.LENGTH);
                    BencodedList path = (BencodedList) file.get(BtKeys.PATH);
                    for (BencodedObject p : path) {
                        BencodedByteSequence pp = (BencodedByteSequence) p;
                        String filePath = pp.toUTF8String();
                        list.add(new MetaFile(filePath, length.getValue()));
                    }
                }
                BencodedByteSequence name = (BencodedByteSequence) map.get(BtKeys.NAME);
                String utf8String = name.toUTF8String();
                return Optional.of(new TorrentMeta(this.torrent, utf8String, list, from));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Optional.empty();
    }

    public Optional<BencodedDictionary> to() {
        return parse(this.bytes.array());
    }
}