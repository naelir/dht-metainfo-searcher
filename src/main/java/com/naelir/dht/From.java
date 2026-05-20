package com.naelir.dht;

public class From {
    final byte[] ip;
    final int port;

    public From(byte[] ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public byte[] getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }
}