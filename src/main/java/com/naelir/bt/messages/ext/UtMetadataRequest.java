package com.naelir.bt.messages.ext;

import java.util.Optional;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.naelir.bt.messages.BtKeys;
import com.naelir.dht.BDecoder;
import com.naelir.dht.BEncoder;

public class UtMetadataRequest extends ExtendedPeerWireMessage {
    public int type;
    public int piece;

    public UtMetadataRequest(int type, int extensionId, int piece) {
        super((byte) extensionId);
        this.type = type;
        this.piece = piece;
    }

    @Override
    public byte[] bencode() {
        BencodedDictionary name = new BencodedDictionary();
        name.put(BtKeys.MSG_TYPE, new BencodedInteger(this.type));
        name.put(BtKeys.PIECE, new BencodedInteger(this.piece));
        return BEncoder.bytes(name);
    }

    @Override
    public void read(byte[] data) {
        Optional<BencodedDictionary> decode = BDecoder.decode(data);
        if (decode.isPresent()) {
            BencodedDictionary map = decode.get();
            BencodedInteger type = (BencodedInteger) map.get(BtKeys.MSG_TYPE);
            this.type = (int) type.getValue();
            BencodedInteger piece = (BencodedInteger) map.get(BtKeys.PIECE);
            this.piece = (int) piece.getValue();
        }
    }

    @Override
    public String toString() {
        return "UtMetadataRequest [type=" + this.type + ", piece=" + this.piece + "]";
    }
}