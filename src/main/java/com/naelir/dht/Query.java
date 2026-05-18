package com.naelir.dht;

import java.util.concurrent.TimeUnit;

import com.naelir.dht.Node.Command;

public class Query {
    Node.Command command;
    long responded;
    long sent;

    public Query(Command command) {
        super();
        this.command = command;
        this.sent = System.currentTimeMillis();
    }

    public boolean expired() {
        return this.sent - this.responded > TimeUnit.SECONDS.toMillis(30);
    }

    public boolean ping() {
        return this.sent == 0 || expired();
    }

    public void responded() {
        this.responded = System.currentTimeMillis();
    }

    public void set(Node.Command command) {
        this.command = command;
    }
}
