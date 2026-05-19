package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    Map<Command, Query> queryMap;

    public Node(byte[] ip, int port, ByteBuffer id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.tid = 1;
        this.queryMap = new ConcurrentHashMap<>();
    }

    public InetAddress address() throws UnknownHostException {
        return InetAddress.getByAddress(this.ip);
    }

    public void command(Command command) {
        this.queryMap.put(command, new Query(command));
    }

    public int nextId() {
        return this.tid++;
    }

    public Query query(Command command) {
        return this.queryMap.get(command);
    }

    enum Command {
        PING, FIND_NODE, GET_PEER, SAMPLE, ANNOUNCE
    }
}
