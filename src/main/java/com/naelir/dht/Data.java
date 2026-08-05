package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.tuple.Pair;

import com.naelir.Arguments;
import com.naelir.bt.Entry;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.db.EntryRepository;
import com.naelir.db.MongoEntryRepository;
import com.naelir.fs.FileDB;
import com.naelir.http.IRemoteClient;

public class Data {
    public ByteBuffer myself;
    public RoutingTable table;
    
    public final Map<ByteBuffer, IRequest> requestsSent;
    public final Map<String, Torrent> torrents;
    public final Map<String, Sample> samples;
    public final Queue<MetaTorrentTask> udptasks;
    public final Map<ByteBuffer, Node> tokensSent;
    public final Map<ByteBuffer, Node> tokensReceived;
    public final String tcpmyself;
    public final FileDB fileManager;
    public final Queue<ByteBuffer> udpIds;
    public final Arguments arguments;
    public final EntryRepository dbRepo;
    public final Deque<MetaTorrentTask> tcptasks;
    public final Set<Pair<String, Pair<String, Integer>>> forUpdate;
    public final Set<String> scrapeHashes;

    public Data(Queue<ByteBuffer> udpIds, String tcpmyself, FileDB fm, Arguments arguments) {
        this.udpIds = udpIds;
        this.arguments = arguments;
        this.dbRepo = getRepo();
        this.myself = udpIds.poll();
        this.tcpmyself = tcpmyself;
        this.scrapeHashes = new HashSet<String>();
        this.requestsSent = new ConcurrentHashMap<>();
        this.torrents = new ConcurrentHashMap<>();
        this.samples = new ConcurrentHashMap<>();
        this.tokensSent = new ConcurrentHashMap<>();
        this.tokensReceived = new ConcurrentHashMap<>();
        this.table = new RoutingTable();
        this.udptasks = new LinkedBlockingQueue<>(5000);
        this.tcptasks = new LinkedBlockingDeque<>(5000);
        this.fileManager = fm;
        this.forUpdate = new HashSet<>();
    }

    EntryRepository getRepo() {
        return this.arguments.connectionString != null
                ? new MongoEntryRepository(this.arguments.connectionString, this.arguments.db, this.arguments.table)
                : new EntryRepository() {
                    @Override
                    public long count() {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public List<Entry> findAll(int page, int pageSize) {
                        // TODO Auto-generated method stub
                        return null;
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
                    public boolean remove(String hash) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public boolean update(Entry entry) {
                        // TODO Auto-generated method stub
                        return false;
                    }
                    @Override
                    public long updateMany(List<String> hashes) {
                        // TODO Auto-generated method stub
                        return 0;
                    }
                };
    }

    public void nextId() {
        this.myself = this.udpIds.poll();
    }
}
