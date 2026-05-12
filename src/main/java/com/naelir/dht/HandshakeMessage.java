package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class HandshakeMessage {
    static final String BITTORENT = "BitTorrent protocol";
    byte protocolLength;
    byte[] reserved;
    byte[] protocol;
    byte[] torrentHash;
    byte[] peerID;

    public HandshakeMessage(ByteBuffer torrentHash, ByteBuffer peerID) {
        this.torrentHash = torrentHash.array();
        this.peerID = peerID.array();
        this.reserved = new byte[8];
        this.protocolLength = 0x13;
        this.protocol = BITTORENT.getBytes();
    }

    public void from(byte[] bytes) {
        this.protocolLength = bytes[0];
        this.protocol = Arrays.copyOfRange(bytes, 1, 20);
        this.reserved = Arrays.copyOfRange(bytes, 20, 28);
        this.torrentHash = Arrays.copyOfRange(bytes, 28, 48);
        this.peerID = Arrays.copyOfRange(bytes, 48, 68);
    }

    public ByteBuffer to() {
        ByteBuffer buffer = ByteBuffer.allocate(68);
        buffer.put(this.protocolLength);
        buffer.put(this.protocol);
        buffer.put(this.reserved);
        buffer.put(this.torrentHash);
        buffer.put(this.peerID);
        return buffer;
    }
}
