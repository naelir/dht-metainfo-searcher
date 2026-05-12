package com.naeir.bt;

import io.netty.buffer.ByteBuf;

public class HandshakeRequest {
    public static final byte[] GET_METADATA_HANDSHAKE_PRE_BYTES = { 19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116,
            32, 112, 114, 111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1 };
    public static final String PROTOCOL = "BitTorrent protocol";
    public byte[] protocol = new byte[19];
    public long reserved = 0x20;
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
        buffer.writeBytes(GET_METADATA_HANDSHAKE_PRE_BYTES);
        buffer.writeBytes(this.torrentHash);
        buffer.writeBytes(this.peerID);
    }
}
