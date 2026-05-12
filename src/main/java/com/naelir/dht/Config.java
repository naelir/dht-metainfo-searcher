package com.naelir.dht;

public class Config {
    static final long NODE_EXPIRE_TIME = 15 * 60 * 1000; // 15 min
    static final long NODE_REPLACEABLE_TIME = 12 * 60 * 1000; // 12 min
    static final long NODE_PING_TIME = 10 * 60 * 1000; // 10 min
    static final long UPDATE_BUCKET_INTERVAL = 10 * 60 * 1000; // 10 min
    static final int MAX_NODES = 400;
    final static int DEFAULT_GETPEERS_DEPTH = 5;
    final static int DEFAULT_GETPEERS_MAXPEERS = 50;
    public static final long PEER_CLEAN_INTERVAL = 60 * 1000; // 60 sec
    public static final long PEER_EXPIRE_TIME = 15 * 60 * 1000; // 15 min
    final static int MAX_INFOHASHES = 20;
    final static int MAX_PEERS_PER_INFOHASH = 10;
    public final static int TOKEN_LENGTH = 8;
    static final int MAX_ANNOUNCE_NODES = 8;
    final static int NUM_RETURN_NODES = 8;
    final static long TOKENS_CLEAN_INTERVAL = 65 * 1000; // 65 sec
    final static long ANNOUNCE_INTERVAL = 70 * 1000; // 70 sec
    final static long TOKEN_TIMEOUT = 10 * 60 * 1000; // 10 mins
}
