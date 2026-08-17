package com.naelir.dht;

import java.nio.ByteBuffer;
import java.time.Duration;
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.naelir.Arguments;
import com.naelir.bt.Entry;
import com.naelir.bt.Torrent;
import com.naelir.db.EntryRepository;
import com.naelir.db.MongoEntryRepository;
import com.naelir.fs.IFileDB;
import com.naelir.fs.ILocationDb;
import com.naelir.tasks.MetaTorrentTask;
import com.naelir.tasks.Sample;

public class Data {
    public ByteBuffer myself;
    public RoutingTable table;
    
    public final Cache<ByteBuffer, IRequest> requestsSent;
    public final Map<String, Torrent> torrents;
    public final Map<String, Sample> samples;
    public final Queue<MetaTorrentTask> udptasks;
    public final Cache<ByteBuffer, Node> tokensSent;
    public final Cache<ByteBuffer, Node> tokensReceived;
    public final String tcpmyself;
    public final IFileDB fileManager;
    public final Queue<ByteBuffer> udpIds;
    public final Arguments arguments;
    public final EntryRepository dbRepo;
    public final Deque<MetaTorrentTask> tcptasks;
    public final Set<Pair<String, Integer>> forUpdate;
    public final List<String> unresolved;
    public final Set<String> scrapeHashes;
    public final ILocationDb locationDb;

    public Data(Queue<ByteBuffer> udpIds, String tcpmyself, IFileDB fm, ILocationDb locationDb, Arguments arguments) {
        this.udpIds = udpIds;
        this.locationDb = locationDb;
        this.arguments = arguments;
        this.dbRepo = getRepo();
        this.myself = udpIds.poll();
        this.tcpmyself = tcpmyself;
        this.scrapeHashes = new HashSet<String>();
        this.unresolved = new ArrayList<>();
        // These maps are keyed by transaction id / token and only ever removed
        // when a matching response/announce arrives. Requests that never get a
        // response (dropped UDP packets, offline nodes, DHT spam, etc.) used to
        // stay in a plain ConcurrentHashMap forever (only fully wiped when the
        // node id rotates), causing unbounded growth => memory leak.
        // Guava caches with a time based expiry bound their size automatically.
        this.requestsSent = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build();
        this.torrents = new ConcurrentHashMap<>();
        this.samples = new ConcurrentHashMap<>();
        this.tokensSent = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
        this.tokensReceived = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
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
                    public long updateMany(List<String> hashes) {
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
                    public long updateMany(List<String> hashes, int newPeerCount) {
                        // TODO Auto-generated method stub
                        return 0;
                    }
                };
    }

    public ByteBuffer nextId() {
        this.myself = this.udpIds.poll();
        this.udpIds.offer(myself);
        return this.myself;
    }
}
