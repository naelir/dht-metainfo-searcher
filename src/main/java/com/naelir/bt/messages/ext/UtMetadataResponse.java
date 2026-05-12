package com.naelir.bt.messages.ext;

import java.util.Optional;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.naelir.bt.messages.BtKeys;
import com.naelir.dht.BDecoder;
import com.naelir.dht.BDecoder.Frame;

public class UtMetadataResponse extends ExtendedPeerWireMessage {
    public int piece;
    private int totalSize;
    int size;
    private byte[] meta;
    public int type;

    public UtMetadataResponse(int type, byte extensionId) {
        super(extensionId);
        this.type = type;
    }

    @Override
    public byte[] bencode() {
        return null;
    }

    @Override
    public void read(byte[] buffer) {
        Optional<Frame> decode = BDecoder.decodeSized(buffer);
        if (decode.isPresent()) {
            Frame frame = decode.get();
            BencodedDictionary map = frame.map;
            this.size = frame.size;
            BencodedInteger type = (BencodedInteger) map.get(BtKeys.MSG_TYPE);
            this.extensionId = (byte) type.getValue();
            BencodedInteger piece = (BencodedInteger) map.get(BtKeys.PIECE);
            this.piece = (int) piece.getValue();
            BencodedInteger totalSize = (BencodedInteger) map.get("total_size");
            if (totalSize != null) {
                this.totalSize = (int) totalSize.getValue();
            }
            int remain = buffer.length - frame.size;
            this.meta = new byte[remain];
            System.arraycopy(buffer, frame.size, this.meta, 0, remain);
        }
    }

    @Override
    public String toString() {
        return "UtMetadataResponse [piece=" + this.piece + ", totalSize=" + this.totalSize + ", size=" + this.size
                + "]";
    }
}
