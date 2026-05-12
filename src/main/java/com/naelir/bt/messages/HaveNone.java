package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public class HaveNone extends AbstractPeerWireMessage {
    public HaveNone() {
        super(BtKeys.HAVE_NONE_ID);
    }

    @Override
    public void readImpl(ByteBuf buffer) {
    }

    @Override
    public String toString() {
        return "HAVENONE";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
    }
}
