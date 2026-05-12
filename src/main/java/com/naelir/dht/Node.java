package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Node {
    public static final Logger logger = LogManager.getLogger(Node.class);

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
//    IpRange range;

    public Node(byte[] ip, int port) {
        this(ip, port, Generator.generateRandomID());
    }

    public Node(byte[] ip, int port, ByteBuffer id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.tid = 1;
        this.queryMap = new ConcurrentHashMap<>();
//        this.range = IpRangeFilter.inRange(ip, IpRangeFilter.RANGES_ALLOW);
    }

    public InetAddress address() {
        try {
            return InetAddress.getByAddress(this.ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public Query get(Command command) {
        return this.queryMap.get(command);
    }

    public int nextId() {
        return this.tid++;
    }

    public int port() {
        return this.port;
    }

    public void put(Command command) {
        this.queryMap.put(command, new Query(command));
        logger.debug("adding {} on node {}", command, this);
    }

    @Override
    public String toString() {
        return "Node [id=" + Generator.toHex(this.id.array()) + /* ", country=" + this.range.country + */ ", ip="
                + Generator.ip(this.ip) + ", port=" + this.port + ", queryMap=" + this.queryMap + "]";
    }

    enum Command {
        PING, FIND_NODE, GET_PEER, SAMPLE, ANNOUNCE
    }
}
