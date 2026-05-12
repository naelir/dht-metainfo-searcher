package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public class KeepAliveMessage implements PeerWireMessage {
    public KeepAliveMessage() {
    }

    @Override
    public void read(ByteBuf buffer) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "KEEP_ALIVE []";
    }

    @Override
    public void write(ByteBuf buffer) {
    }
}
