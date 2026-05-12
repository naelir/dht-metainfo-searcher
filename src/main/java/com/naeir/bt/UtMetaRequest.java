package com.naeir.bt;

import com.naelir.dht.BEncoder;

import io.netty.buffer.ByteBuf;

public class UtMetaRequest {
    public Integer type = 0;
    public Integer piece = 0;
    public int code;

    public UtMetaRequest(int code) {
        this.code = code;
    }

    public void write(ByteBuf buffer) {
        byte[] meta = BEncoder.encode(this);
        buffer.writeInt(meta.length + 2);
        buffer.writeByte(20);
        buffer.writeByte(this.code);
        buffer.writeBytes(meta);
    }
}