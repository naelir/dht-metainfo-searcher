package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Data {
    static List<Node> parse(ByteBuffer nodes, InetAddress address) {
        int ipLength = address.getAddress().length;
        List<Node> list = new LinkedList<>();
        int compactElementSize = 20 + ipLength + 2;
        if (nodes == null || nodes.array().length % compactElementSize != 0)
            return Collections.emptyList();
        int numNodes = nodes.array().length / compactElementSize;
        for (int i = 0; i < numNodes; i++) {
            byte[] id = new byte[20];
            nodes.get(id);
            byte[] ip = new byte[ipLength];
            nodes.get(ip);
            byte[] portBytes = new byte[2];
            nodes.get(portBytes);
            int port = ((portBytes[0] & 0xFF) << 8) | (portBytes[1] & 0xFF);
            list.add(new Node(ip, port, ByteBuffer.wrap(id)));
        }
        return list;
    }

    Buckets buckets;
    Map<CommandId, IRequest> commandsSent;
    Map<ByteBuffer, Node> nodes;
    Map<ByteBuffer, Torrent> torrents;
    ByteBuffer myself;
    Map<ByteBuffer, Token> tokens;

    public Data(ByteBuffer myself) {
        this.myself = myself;
        this.commandsSent = new ConcurrentHashMap<>();
        this.nodes = new ConcurrentHashMap<>();
        this.torrents = new ConcurrentHashMap<>();
        this.buckets = new Buckets(myself);
    }

    public List<Node> closest(ByteBuffer id, int max) {
        List<ByteBuffer> ids = this.buckets.getClosest(id, 2 * max);
        List<Node> closest = new ArrayList<>(ids.size());
        for (ByteBuffer key : ids) {
            Node node = this.nodes.get(key);
            if (node != null && !node.isUnsafe && closest.size() < max) {
                closest.add(node);
            }
        }
        if (closest.size() < max) {
            for (ByteBuffer key : ids) {
                Node node = this.nodes.get(key);
                if (node != null && node.isUnsafe && closest.size() < max) {
                    closest.add(node);
                }
            }
        }
        return closest;
    }
}
