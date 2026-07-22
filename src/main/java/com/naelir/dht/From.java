package com.naelir.dht;

public class From {
    public final byte[] ip;
    public final int port;

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

    @Override
    public String toString() {
        return "From [ip=" + Generator.inet(this.ip) + ", port=" + this.port + "]";
    }
}