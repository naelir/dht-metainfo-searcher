package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public class PortMessage extends AbstractPeerWireMessage {
    public int listenPort;

    public PortMessage() {
        super(BtKeys.PORT_MESSAGE_ID);
    }

    public PortMessage(int listenPort) {
        super(BtKeys.PORT_MESSAGE_ID);
        this.listenPort = listenPort;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        this.listenPort = buffer.readShort();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PORT [listenPort=" + this.listenPort + "]";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
        buffer.writeInt(this.listenPort);
    }
}
