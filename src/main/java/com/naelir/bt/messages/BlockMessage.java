package com.naelir.bt.messages;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

public class BlockMessage extends AbstractPeerWireMessage {
    public int pieceIndex;
    public int begin;
    public ByteBuffer data;

    public BlockMessage() {
        super(BtKeys.BLOCK_MESSAGE_ID);
    }

    public BlockMessage(int pieceIndex, int begin, ByteBuffer data) {
        super(BtKeys.BLOCK_MESSAGE_ID);
        this.pieceIndex = pieceIndex;
        this.begin = begin;
        this.data = data;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        this.pieceIndex = buffer.readInt();
        this.begin = buffer.readInt();
        this.data = buffer.readBytes(buffer.readableBytes()).nioBuffer();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BLOCK [pieceIndex=" + this.pieceIndex + ", begin=" + this.begin + ", data=" + this.data + "]";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
        buffer.writeInt(this.pieceIndex);
        buffer.writeInt(this.begin);
        buffer.writeBytes(this.data);
    }
}
