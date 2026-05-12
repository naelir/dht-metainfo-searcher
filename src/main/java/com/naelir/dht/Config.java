package com.naelir.dht;

public class Config {
    static final long CLEAN_INTERVAL = 60 * 1000; // 60 sec
    static final long NODE_EXPIRE_TIME = 15 * 60 * 1000; // 15 min
    static final long NODE_REPLACEABLE_TIME = 12 * 60 * 1000; // 12 min
    static final long NODE_PING_TIME = 10 * 60 * 1000; // 10 min
    static final long EXPLORE_INTERVAL = 60 * 1000; // 60 sec
    static final long UPDATE_BUCKET_INTERVAL = 10 * 60 * 1000; // 10 min
    static final int EXPLORE_AGGRESSIVE_MAX_NODES = 100;
    static final int EXPLORE_MAX_NODES = 600;
    static final int MAX_NODES = 800;
    public static final int ID_SIZE = 160;
    public static final long DEFAULT_QUERY_TIMEOUT = 60 * 1000; // 60 sec
    final static int DEFAULT_GETPEERS_DEPTH = 5;
    final static int DEFAULT_GETPEERS_MAXPEERS = 50;
    public static final long PEER_CLEAN_INTERVAL = 60 * 1000; // 60 sec
    public static final long PEER_EXPIRE_TIME = 15 * 60 * 1000; // 15 min
    final static int MAX_INFOHASHES = 20;
    final static int MAX_PEERS_PER_INFOHASH = 10;
    public final static int TOKEN_LENGTH = 8;
    static final int MAX_ANNOUNCE_NODES = 8;
    static final int MAX_QUERY_TIME = 60 * 1000; // 60 sec
    static final int MAX_MESSAGE_ID = 0xffff;
    private final static long TICK_THREAD_SLEEP = 5000; // 5 sec
    final static int NUM_RETURN_NODES = 8;
    private final static long EXPLORER_NODE_INTERVAL = 3000; // 3 sec
    final static long BLACKLIST_CLEAN_INTERVAL = 60 * 1000; // 60 sec
    final static long TOKENS_CLEAN_INTERVAL = 65 * 1000; // 65 sec
    final static long ANNOUNCE_INTERVAL = 70 * 1000; // 70 sec
    final static long BLACKLIST_TIMEOUT = 10 * 60 * 1000; // 10 mins
    final static long TOKEN_TIMEOUT = 10 * 60 * 1000; // 10 mins
    public static final int BUCKET_SIZE = 2;
}
