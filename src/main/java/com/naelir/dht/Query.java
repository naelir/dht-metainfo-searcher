package com.naelir.dht;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.naelir.dht.Node.Command;

public class Query {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    Node.Command command;
    long responded;
    long sent;
    private long nextTryIn;

    private int count;

    public Query(Command command) {
        super();
        this.count = COUNTER.incrementAndGet();
        this.command = command;
        this.sent = System.currentTimeMillis();
        this.nextTryIn = 0;
    }

    public long getResponded() {
        return this.responded;
    }

    public boolean notResponding() {
        return this.responded == 0 && (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30) > this.sent);
    }

    public void responded(TimeUnit unit, Integer interval) {
        this.responded = System.currentTimeMillis();
        this.nextTryIn = this.responded + unit.toMillis(interval);
    }

    public boolean responding() {
        return this.responded - this.sent >= 0;
    }

    public void setResponded() {
        this.responded = System.currentTimeMillis();
    }

    public boolean shouldRecheck() {
        return System.currentTimeMillis() - this.nextTryIn > 0;
    }

    @Override
    public String toString() {
        return "Query [count=" + count + ", command=" + command + ", responded=" + responded + "]";
    }

}
