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

    int tid;
    byte[] ip;
    int port;
    ByteBuffer id;
    List<ByteBuffer> torrents;
    long lastSeen;
    long nextRequestTime;
    boolean asked;

    public Node(byte[] ip, int port, ByteBuffer id) {
        this(ip, port, id, Collections.emptyList());
    }

    public Node(byte[] ip, int port, ByteBuffer id, List<ByteBuffer> torrents) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.torrents = torrents;
        this.tid = 1;
        this.lastSeen = 0;
        this.asked = false;
        this.nextRequestTime = 0;
    }

    public InetAddress address() throws UnknownHostException {
        return InetAddress.getByAddress(this.ip);
    }

    public boolean expired() {
        return this.lastSeen != 0 && System.currentTimeMillis() - this.lastSeen > Config.NODE_EXPIRE_TIME;
    }

    public boolean forPing() {
        long l = System.currentTimeMillis() - this.nextRequestTime;
        this.nextRequestTime = System.currentTimeMillis() + Config.NODE_PING_TIME;
        return l > 0;
    }

    public int nextId() {
        return this.tid++;
    }

    public void replyOn(IResponse key) {
        if (key instanceof SampleInfoHashesResponse sihr) {
            this.nextRequestTime = System.currentTimeMillis() + sihr.interval * 1000;
        } else if (key instanceof PingResponse) {
            this.nextRequestTime = System.currentTimeMillis() + Config.NODE_PING_TIME;
        }
    }

    enum Command {
        PING, FIND_NODE, GET_PEER, SAMPLE, ANNOUNCE
    }
}
