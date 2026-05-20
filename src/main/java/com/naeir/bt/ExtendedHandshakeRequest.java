package com.naeir.bt;

import java.util.Optional;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.naelir.dht.BDecoder;
import com.naelir.dht.BEncoder;

import io.netty.buffer.ByteBuf;

public class ExtendedHandshakeRequest {
    public int utCode;

    public ExtendedHandshakeRequest() {
        // TODO Auto-generated constructor stub
    }

    public ExtendedHandshakeRequest(int utCode) {
        this.utCode = utCode;
    }

    public void read(ByteBuf buffer) {
        int length = buffer.readInt();
        byte twenty0 = buffer.readByte();
        byte zero0 = buffer.readByte();
        byte[] data = new byte[length - 2];
        buffer.readBytes(data);
        Optional<BencodedDictionary> decode = BDecoder.decode(data);
        if (decode.isPresent()) {
            BencodedDictionary map = decode.get();
//            System.out.println(Convert.to(map));
            BencodedDictionary mkey = (BencodedDictionary) map.get("m");
            BencodedInteger code = (BencodedInteger) mkey.get("ut_metadata");
            this.utCode = (int) code.getValue();
        }
        int readableBytes = buffer.readableBytes();
        byte[] wtf = new byte[readableBytes];
        buffer.readBytes(wtf);
    }

    public void write(ByteBuf buffer) {
        byte[] meta = BEncoder.encode(this);
        buffer.writeInt(meta.length + 2);
        buffer.writeByte(20);
        buffer.writeByte(0);
        buffer.writeBytes(meta);
    }
}
