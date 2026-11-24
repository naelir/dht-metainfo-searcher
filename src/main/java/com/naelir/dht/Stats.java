package com.naelir.dht;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Stats {
    public static final Logger logger = LogManager.getLogger(Stats.class);

    public final AtomicInteger received;
    public final AtomicInteger resolved;
    public final AtomicInteger searchable;
    public final AtomicInteger tooFar;

    public Stats() {
        this.received = new AtomicInteger(0);
        this.tooFar = new AtomicInteger(0);
        this.resolved = new AtomicInteger(0);
        this.searchable = new AtomicInteger(0);
    }

    public void clear() {
        this.received.set(0);
        this.tooFar.set(0);
        this.resolved.set(0);
        this.searchable.set(0);
    }
    
    public void log() {
        logger.info("Stats: received={}, resolved={}, searchable={}, tooFar={}, percentResolved={}", 
            this.received.get(), this.resolved.get(), this.searchable.get(), this.tooFar.get(), this.resolved.get() * 100.0 / (this.received.get() + 1));
    }

}
