package com.naelir.bt;

import com.naelir.bt.messages.AbstractPeerWireMessage;

import io.netty.buffer.ByteBuf;

public class Unknown extends AbstractPeerWireMessage {
    private int length;

    public Unknown(int messageID, int length) {
        super(messageID);
        this.length = length;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        byte[] name = new byte[this.length];
        buffer.readBytes(name);
    }
}
