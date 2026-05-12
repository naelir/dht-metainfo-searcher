package com.naelir.bt.messages.ext;

import io.netty.buffer.ByteBuf;

public abstract class ExtendedPeerWireMessage implements IExtendedPeerWireMessage {
    byte extensionId;

    public ExtendedPeerWireMessage(byte extensionId) {
        this.extensionId = extensionId;
    }

    abstract public byte[] bencode();

    @Override
    public void write(ByteBuf buffer) {
        byte[] meta = bencode();
        buffer.writeInt(meta.length + 2);
        buffer.writeByte(20);
        buffer.writeByte(this.extensionId);
        buffer.writeBytes(meta);
    }
}