package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.naelir.bt.Torrent;
import com.naelir.dht.Converter;
import com.naelir.dht.Node;

public class Sample {
    Torrent torrent;
    Set<Node> asked;
    Set<Node> peers;
    public boolean skip;
    Set<String> deniedLocations;
    public int seeders;
    
    public Sample(Torrent torrent, List<Node> ask, boolean skip) {
        this.torrent = torrent;
        this.peers = new HashSet<>();
        this.asked = new HashSet<>(4);
        this.deniedLocations = new HashSet<>(4);
    }

    public synchronized Set<String> deniedLocations() {
        return new HashSet<>(this.deniedLocations);
    }
    
    public synchronized void addLocation(String location) {
        this.deniedLocations.add(location);
    }
    
    public synchronized void addAsked(Node n) {
        this.asked.add(n);
    }
    
    public synchronized void addPeer(Node list) {
        this.peers.add(list);
    }

    public synchronized boolean asked(Node node) {
        return this.asked.contains(node);
    }

    public ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(Converter.toArray(this.torrent.infoHash()));
    }

    public synchronized Set<Node> peers() {
        return new HashSet<>(this.peers);
    }

    public Torrent torrent() {
        return this.torrent;
    }

    @Override
    public String toString() {
        return "Sample [torrent=" + torrent.infoHash() + ", resolved=" + String.valueOf(torrent.meta() != null) + ", asked=" + asked.size() + ", peers=" + peers.size()+ ", seeders=" + seeders + ", skip=" + skip
                + ", deniedLocations=" + deniedLocations + "]";
    }
    
    
}
