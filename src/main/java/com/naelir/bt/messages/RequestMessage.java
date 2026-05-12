package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public class RequestMessage extends AbstractPeerWireMessage {
    public int pieceIndex;
    public int begin;
    public int length;

    public RequestMessage() {
        super(BtKeys.REQ_MESSAGE_ID);
    }

    public RequestMessage(int pieceIndex, int begin, int length) {
        super(BtKeys.REQ_MESSAGE_ID);
        this.pieceIndex = pieceIndex;
        this.begin = begin;
        this.length = length;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        this.pieceIndex = buffer.readInt();
        this.begin = buffer.readInt();
        this.length = buffer.readInt();
    }

    @Override
    public String toString() {
        return "REQUEST [pieceIndex=" + this.pieceIndex + ", begin=" + this.begin + ", length=" + this.length + "]";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
        buffer.writeInt(this.pieceIndex);
        buffer.writeInt(this.begin);
        buffer.writeInt(this.length);
    }
}
