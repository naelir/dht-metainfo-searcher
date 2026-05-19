package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.naeir.bt.Torrent;

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

    RoutingTable table;
    Map<CommandId, IRequest> sent;
    Map<ByteBuffer, Torrent> torrents;
    ByteBuffer myself;
    Map<ByteBuffer, Node> tokensSent;
    Map<ByteBuffer, Node> tokensReceived;

    public Data(ByteBuffer myself) {
        this.myself = myself;
        this.sent = new ConcurrentHashMap<>();
        this.torrents = new ConcurrentHashMap<>();
        this.tokensSent = new ConcurrentHashMap<>();
        this.tokensReceived = new ConcurrentHashMap<>();
        this.table = new RoutingTable(myself);
    }
}
