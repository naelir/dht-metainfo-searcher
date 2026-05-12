package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class Node {
    public static Node of(ByteBuffer compactInfo, int ipLength) {
        byte[] rawId = new byte[20];
        compactInfo.get(rawId);
        byte[] rawIp = new byte[ipLength];
        compactInfo.get(rawIp);
        byte[] rawPort = new byte[2];
        compactInfo.get(rawPort);
        ByteBuffer id = ByteBuffer.wrap(rawId);
        int port = ((rawPort[0] & 0xFF) << 8) | (rawPort[1] & 0xFF);
        return new Node(rawIp, port, id);
    }

    byte[] ip;
    int port;
    ByteBuffer id;
    List<ByteBuffer> torrents;
    boolean isUnsafe;

    public Node(byte[] ip, int port, ByteBuffer id) {
        this(ip, port, id, Collections.emptyList());
    }

    public Node(byte[] ip, int port, ByteBuffer id, List<ByteBuffer> torrents) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.torrents = torrents;
    }

    public InetAddress address() throws UnknownHostException {
        return InetAddress.getByAddress(this.ip);
    }
}
