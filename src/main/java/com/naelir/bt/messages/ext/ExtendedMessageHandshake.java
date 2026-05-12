package com.naelir.bt.messages.ext;

import java.util.Optional;

import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.naelir.dht.BDecoder;
import com.naelir.dht.BEncoder;
import com.naelir.dht.Generator;

public class ExtendedMessageHandshake extends ExtendedPeerWireMessage {
    public int ut_metadata = 1;
    public String version = "SZ 1.0.1";
    public int metadata_size;
    public int reqq;
    public int upload_only;
    byte[] yourip;
    String ipv4;

    public ExtendedMessageHandshake() {
        super((byte) 0);
    }

    public ExtendedMessageHandshake(byte[] yourip) {
        super((byte) 0);
        this.yourip = yourip;
    }

    @Override
    public byte[] bencode() {
        BencodedDictionary parent = new BencodedDictionary();
        BencodedDictionary child = new BencodedDictionary();
        child.put("ut_metadata", new BencodedInteger(this.ut_metadata));
        parent.put("m", child);
//        parent.put("p", new BencodedInteger(this.port));
        parent.put("reqq", new BencodedInteger(250));
        parent.put("v", new BencodedByteSequence(this.version));
        parent.put("yourip", new BencodedByteSequence(Generator.toHex(this.yourip)));
        return BEncoder.bytes(parent);
    }

    @Override
    public void read(byte[] data) {
        Optional<BencodedDictionary> decode = BDecoder.decode(data);
        if (decode.isPresent()) {
            BencodedDictionary map = decode.get();
            BencodedDictionary mkey = (BencodedDictionary) map.get("m");
            BencodedInteger code = (BencodedInteger) mkey.get("ut_metadata");
            BencodedByteSequence version = (BencodedByteSequence) map.get("v");
            BencodedInteger ms = (BencodedInteger) map.get("metadata_size");
            if (code != null) {
                this.ut_metadata = (int) code.getValue();
            }
            if (version != null) {
                this.version = version.toUTF8String();
            }
            if (ms != null) {
                this.metadata_size = (int) ms.getValue();
            }
        }
    }

    @Override
    public String toString() {
        return "ExtendedHandshake [version=" + this.version + ", ut_metadata=" + this.ut_metadata + ", metadata_size="
                + this.metadata_size + "]";
    }
}