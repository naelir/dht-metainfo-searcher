package com.naelir.bt.messages;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;

public class HandshakeMessage implements PeerWireMessage {
    private static final long RESERVED = 1 | 1 << 20;
    private static final long RESERVED0 = 1 << 20;
    public int protocolStringLength = 19;
    public String protocolString = "BitTorrent protocol";
    public long reserved = 0;
    public byte[] torrentHash = new byte[20];
    public String peerID;

    public HandshakeMessage() {
    }

    public HandshakeMessage(byte[] torrentHash, String peerID) {
        this.torrentHash = torrentHash;
        this.peerID = peerID;
    }

    public boolean isExtended() {
        return (this.reserved & RESERVED0) != 0;
    }

    @Override
    public void read(ByteBuf buffer) {
        this.protocolStringLength = buffer.readByte();
        byte[] str = new byte[this.protocolStringLength];
        buffer.readBytes(str);
        this.protocolString = new String(str);
        // protocolString = buffer.readBytes
        this.reserved = buffer.readLong();
        byte[] pi = new byte[20];
        buffer.readBytes(this.torrentHash, 0, 20);
        buffer.readBytes(pi, 0, 20);
        this.peerID = new String(pi);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "HANDSHAKE [protocolStringLength=" + this.protocolStringLength + ", protocolString="
                + this.protocolString + ", reserved=" + this.reserved + ", torrentHash="
                + Arrays.toString(this.torrentHash) + ", peerID=" + this.peerID + "]";
    }

    @Override
    public void write(ByteBuf buffer) {
        buffer.writeByte(this.protocolStringLength);
        buffer.writeBytes(this.protocolString.getBytes());
        buffer.writeLong(RESERVED);
        buffer.writeBytes(this.torrentHash, 0, 20);
        buffer.writeBytes(this.peerID.getBytes(), 0, 20);
    }
}
