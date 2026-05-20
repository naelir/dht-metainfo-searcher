package com.naeir.bt;

import io.netty.buffer.ByteBuf;

public class HandshakeRequest {
    private static final long RESERVED = 1 | 1 << 20;
    private static final String PROTOCOL = "BitTorrent protocol";
    public byte[] protocol = new byte[19];
    public long reserved;
    public byte[] torrentHash;
    public byte[] peerID;

    public HandshakeRequest() {
        this(new byte[20], new byte[20]);
    }

    public HandshakeRequest(byte[] torrentHash, byte[] peerID) {
        this.torrentHash = torrentHash;
        this.peerID = peerID;
    }

    public void read(ByteBuf buffer) {
        byte length = buffer.readByte();
        buffer.readBytes(this.protocol);
        this.reserved = buffer.readLong();
        buffer.readBytes(this.torrentHash);
        buffer.readBytes(this.peerID);
    }

    public void write(ByteBuf buffer) {
        buffer.writeByte(PROTOCOL.length());
        buffer.writeBytes(PROTOCOL.getBytes());
        buffer.writeLong(RESERVED);
        buffer.writeBytes(this.torrentHash);
        buffer.writeBytes(this.peerID);
    }
}
