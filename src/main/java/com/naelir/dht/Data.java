package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.naelir.Arguments;
import com.naelir.bt.Entry;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.db.EntryRepository;
import com.naelir.db.MongoEntryRepository;
import com.naelir.fs.FileManager;
import com.naelir.http.IRemoteClient;

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

    public RoutingTable table;
    public Map<ByteBuffer, IRequest> sent;
    public Map<String, Torrent> torrents;
//    public Set<String> unresolved;
    public Map<String, Sample> samples;
    public Queue<MetaTorrentTask> tasks;
    public ByteBuffer myself;
    Map<ByteBuffer, Node> tokensSent;
    Map<ByteBuffer, Node> tokensReceived;
    String tcpmyself;
    public FileManager fm;
    public IRemoteClient remoteClient;
    public Queue<ByteBuffer> udpIds;
    public final Arguments arguments;
    public final EntryRepository repo;

    public Data(Queue<ByteBuffer> udpIds, String tcpmyself, FileManager fm, Arguments arguments) {
        this.udpIds = udpIds;
        this.repo = getRepo();
        this.arguments = arguments;
        this.myself = udpIds.poll();
        this.tcpmyself = tcpmyself;
        this.sent = new ConcurrentHashMap<>();
        this.torrents = new ConcurrentHashMap<>();
        this.samples = new ConcurrentHashMap<>();
        this.tokensSent = new ConcurrentHashMap<>();
        this.tokensReceived = new ConcurrentHashMap<>();
        this.table = new RoutingTable();
//        this.unresolved = new HashSet<>();
        this.tasks = new ArrayBlockingQueue<>(5000);
        this.remoteClient = new IRemoteClient() {
            @Override
            public void saveMeta(String hash, TorrentMeta meta) {
                // TODO Auto-generated method stub
            }
        };
        this.fm = fm;
    }

    public String getTcpmyself() {
        return this.tcpmyself;
    }

    public void nextId() {
        this.myself = this.udpIds.poll();
    }
    


    EntryRepository getRepo() {
        return arguments.connectionString != null ? new MongoEntryRepository(arguments.connectionString, arguments.db, arguments.table) : new EntryRepository() {

            @Override
            public List<Entry> findAll(int page, int pageSize) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long count() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Entry findByHash(String hash) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<Entry> findByName(String pattern) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Entry insert(Entry entry) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean update(Entry entry) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean remove(String hash) {
                // TODO Auto-generated method stub
                return false;
            }
           
        };
    }
}
