package com.naelir.bt;

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
import com.naelir.bt.messages.BtKeys;
import com.naelir.dht.BDecoder;

public class TorrentMeta {
    public static final Logger logger = LogManager.getLogger(TorrentMeta.class);
    public static final byte[] PIECES = new byte[] { 54, 58, 112, 105, 101, 99, 101, 115 };
    public static final byte[] EE = new byte[] { 101, 101 };
    public static final byte[] BYTE20 = new byte[] { 20, 1 };
    public static final TorrentMeta EMPTY = new TorrentMeta("EMPTY", List.of());
    public static final TorrentMeta SCAM = new TorrentMeta("SCAM", List.of());
    public static final TorrentMeta RESOLVED = new TorrentMeta("RESOLVED", List.of());

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

    public static Optional<TorrentMeta> of(Optional<BencodedDictionary> meta) {
        if (meta.isPresent()) {
            try {
                BencodedDictionary map = meta.get();
                List<MetaFile> list = new ArrayList<>();
                BencodedByteSequence name = (BencodedByteSequence) map.get(BtKeys.NAME);
                BencodedList files = (BencodedList) map.get(BtKeys.FILES);
                if (files != null) {
                    for (BencodedObject e : files) {
                        BencodedDictionary file = (BencodedDictionary) e;
                        BencodedInteger length = (BencodedInteger) file.get(BtKeys.LENGTH);
                        BencodedList path = (BencodedList) file.get(BtKeys.PATH);
                        for (BencodedObject p : path) {
                            BencodedByteSequence pp = (BencodedByteSequence) p;
                            String filePath = pp.toUTF8String();
                            if (".pad".equals(filePath)) {
                                // ignore BEP0049
                                break;
                            }
                            list.add(new MetaFile(filePath, length.getValue()));
                        }
                    }
                    String utf8String = name.toUTF8String();
                    return Optional.of(new TorrentMeta(utf8String, list));
                } else if (name != null) {
                    BencodedInteger length = (BencodedInteger) map.get(BtKeys.LENGTH);
                    String utf8String = name.toUTF8String();
                    list.add(new MetaFile(utf8String, length.getValue()));
                    return Optional.of(new TorrentMeta(utf8String, list));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Optional.empty();
    }

    public static Optional<BencodedDictionary> parse(byte[] array) {
        int indexOf = indexOf(array, PIECES, 0);
        if (indexOf < 0)
            return Optional.empty();
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

    private String name;
    private List<MetaFile> list;
    long found;
    Genre genre;

    public TorrentMeta(String name, List<MetaFile> list) {
        this.name = name;
        this.list = list;
        this.found = System.currentTimeMillis();
        this.genre = NameFilter.from(name, list);
    }

    public long getFound() {
        return this.found;
    }

    public Genre getGenre() {
        return this.genre;
    }

    public List<MetaFile> getList() {
        return this.list;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "TorrentMeta [name=" + this.name + ", list=" + this.list + "]";
    }

    public enum Genre {
        MOVIE_VIDEO, TV, MUSIC, GAME_PC, GAME_PLAYSTATION, GAME_NINTENDO, GAME_XBOX, SOFTWARE, UNKNOWN, XXX;
    }

    public static final class MetaFile {
        String path;
        long bytes;

        public MetaFile(String path, long bytes) {
            this.path = path;
            this.bytes = bytes;
        }

        public long getBytes() {
            return this.bytes;
        }

        public String getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            return "MetaFile [path=" + this.path + ", bytes=" + this.bytes + "]";
        }
    }
}
