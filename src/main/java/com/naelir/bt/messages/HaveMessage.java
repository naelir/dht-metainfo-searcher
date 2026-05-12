package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public class HaveMessage extends AbstractPeerWireMessage {
    public int pieceIndex;

    public HaveMessage() {
        super(BtKeys.HAVE_MESSAGE_ID);
    }

    public HaveMessage(int pieceIndex) {
        super(BtKeys.HAVE_MESSAGE_ID);
        this.pieceIndex = pieceIndex;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        this.pieceIndex = buffer.readInt();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "HAVE [pieceIndex=" + this.pieceIndex + "]";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
        buffer.writeInt(this.pieceIndex);
    }
}
