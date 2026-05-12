package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.naelir.bt.Torrent;

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
    Map<ByteBuffer, IRequest> sent;
    public Map<String, Torrent> torrents;
    public Set<String> unresolved;
    Deque<SampleInfoHashesResponse> samples;
    public Queue<MetaTorrentTask> tasks;
    public List<PingPeersTorrentTask> pingTasks;
    ByteBuffer myself;
    Map<ByteBuffer, Node> tokensSent;
    Map<ByteBuffer, Node> tokensReceived;
    String tcpmyself;
    public FileManager fm;
    public Set<InetAddress> denied;

    public Data(ByteBuffer myself, String tcpmyself, FileManager fm) {
        this.myself = myself;
        this.tcpmyself = tcpmyself;
        this.sent = new ConcurrentHashMap<>();
        this.torrents = new ConcurrentHashMap<>();
        this.samples = new ArrayDeque<>(500);
        this.tokensSent = new ConcurrentHashMap<>();
        this.tokensReceived = new ConcurrentHashMap<>();
        this.table = new RoutingTable(myself);
        this.unresolved = new HashSet<>();
        this.denied = new HashSet<>();
        this.tasks = new ArrayBlockingQueue<>(5000);
        this.pingTasks = new CopyOnWriteArrayList<>();
//        this.tasks = new PriorityQueue<>(5000, new Comparator<ResolveTorrentTask>() {
//            @Override
//            public int compare(ResolveTorrentTask o1, ResolveTorrentTask o2) {
//                return Integer.compare(o1.torrent.peers().size(), o2.torrent.peers().size());
//            }
//        }) {
//            private static final long serialVersionUID = 8689824993615653687L;
//
//            @Override
//            public synchronized boolean offer(ResolveTorrentTask e) {
//                return super.offer(e);
//            }
//
//            @Override
//            public synchronized ResolveTorrentTask poll() {
//                return super.poll();
//            }
//        };
        this.fm = fm;
    }

    public String getTcpmyself() {
        return this.tcpmyself;
    }
}
