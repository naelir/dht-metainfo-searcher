package com.naelir.dht;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.naelir.Config;
import com.naelir.bt.Entry;
import com.naelir.bt.Torrent;
import com.naelir.db.EntryRepository;
import com.naelir.fs.IFileDB;
import com.naelir.fs.ILocationDb;
import com.naelir.tasks.MetaTorrentTask;
import com.naelir.tasks.Sample;

public class Data {
    public ByteBuffer myself;
    public final RoutingTable table;
    
    public final Cache<ByteBuffer, IRequest> requestsSent;
    public final Map<String, Torrent> torrents;
    public final Map<String, Sample> samples;
    public final Queue<MetaTorrentTask> udptasks;
    public final Cache<ByteBuffer, Node> tokensSent;
    public final Cache<ByteBuffer, Node> tokensReceived;
    public final String tcpmyself;
    public final IFileDB fileManager;
    public final Queue<ByteBuffer> udpIds;
    public final Config config;
    public final EntryRepository dbRepo;
    public final Deque<MetaTorrentTask> tcptasks;
    public final Set<Entry> forUpdate;
    public final Set<Entry> forRemoval;
    public final List<Entry> unresolved;
    public final Set<Entry> scrapeHashes;
    public final ILocationDb locationDb;
    public final Stats stats;

    public Data(Queue<ByteBuffer> udpIds, String tcpmyself, IFileDB fm, ILocationDb locationDb, EntryRepository repo, Config arguments) {
        this.udpIds = udpIds;
        this.myself = udpIds.poll();
        this.tcpmyself = tcpmyself;
        this.locationDb = locationDb;
        this.config = arguments;
        this.fileManager = fm;
        this.dbRepo = repo;
        this.scrapeHashes = new HashSet<>();
        this.unresolved = new ArrayList<>();
        // These maps are keyed by transaction id / token and only ever removed
        // when a matching response/announce arrives. Requests that never get a
        // response (dropped UDP packets, offline nodes, DHT spam, etc.) used to
        // stay in a plain ConcurrentHashMap forever (only fully wiped when the
        // node id rotates), causing unbounded growth => memory leak.
        // Guava caches with a time based expiry bound their size automatically.
        this.requestsSent = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build();
        this.torrents = new ConcurrentHashMap<>();
        this.samples = Collections.synchronizedMap(new TreeMap<>());
        this.tokensSent = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
        this.tokensReceived = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
        this.table = new RoutingTable();
        this.udptasks = new LinkedBlockingQueue<>(5000);
        this.tcptasks = new LinkedBlockingDeque<>(5000);
        this.forUpdate = new HashSet<>();
        this.forRemoval = new HashSet<>();
        this.stats = new Stats();
    }

    public ByteBuffer nextId() {
        this.myself = this.udpIds.poll();
        this.udpIds.offer(this.myself);
        return this.myself;
    }
    
    public void clear() {
        scrapeHashes.clear();
        requestsSent.invalidateAll();
        tokensSent.invalidateAll();
        tokensReceived.invalidateAll();
        torrents.clear();
        samples.clear();
        forUpdate.clear();
        forRemoval.clear();
        table.clear();
        stats.clear();
    }
}
