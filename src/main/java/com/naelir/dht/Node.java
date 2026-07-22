package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Node {
    public static final Logger logger = LogManager.getLogger(Node.class);
    private static final AtomicInteger COUNTER = new AtomicInteger();

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
    private int c;
    public Queue<Command> queries;

    public Node(byte[] ip, int port) {
        this(ip, port, Generator.generateRandomID());
    }

    public Node(byte[] ip, int port, ByteBuffer id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.tid = 1;
        this.c = COUNTER.incrementAndGet();
        this.queries = new LinkedList<>();
    }

    public byte[] ip() {
        return ip;
    }
    
    public InetAddress address() {
        try {
            return InetAddress.getByAddress(this.ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        return Arrays.equals(this.ip, other.ip) && this.port == other.port;
    }

    public int getCounter() {
        return this.c;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.ip);
        result = prime * result + Objects.hash(this.port);
        return result;
    }

    public boolean have(Command command) {
        return this.queries.contains(command);
    }

    public int nextId() {
        return this.tid++;
    }

    public int port() {
        return this.port;
    }

    public void put(Command command) {
        this.queries.add(command);
    }

    @Override
    public String toString() {
        return "Node [id=" + this.c + ", ip=" + Generator.ip(this.ip) + ", port=" + this.port;
    }
}
