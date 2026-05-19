package com.naelir.dht;

import java.util.concurrent.TimeUnit;

import com.naelir.dht.Node.Command;

public class Query {
    Node.Command command;
    long responded;
    long sent;
    private long delay;

    public Query(Command command) {
        super();
        this.command = command;
        this.sent = System.currentTimeMillis();
        this.delay = 0;
    }

    public boolean notResponding() {
        return this.sent - this.responded > TimeUnit.SECONDS.toMillis(30);
    }

    public void responded() {
        this.responded = System.currentTimeMillis();
    }

    public void responded(TimeUnit unit, Integer interval) {
        this.responded = System.currentTimeMillis();
        this.delay = this.responded + unit.toMillis(interval);
    }

    public boolean shouldRecheck() {
        return System.currentTimeMillis() - this.delay - this.sent > 0;
    }
}
