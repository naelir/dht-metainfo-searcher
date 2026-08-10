package com.naelir.bt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import com.naelir.dht.Node;

public class Torrent {
    public static final Torrent EMPTY = new Torrent("0");

    public static Torrent empty(String hash) {
        Torrent name = new Torrent(hash);
        name.meta = new TorrentMeta(hash);
        return name;
    }

    boolean active;
    String infoHash;
    TorrentMeta meta;
    Deque<Node> peers;
    
    public Torrent(String infoHash, TorrentMeta meta) {
        this.infoHash = infoHash;
        this.peers = new ArrayDeque<>(20);
        this.meta = meta;
    }
    
    public Torrent(String infoHash) {
        this(infoHash, null);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Torrent addPeer(Node node) {
        this.peers.add(node);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Torrent other = (Torrent) obj;
        return Objects.equals(this.infoHash, other.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.infoHash);
    }

    public String infoHash() {
        return this.infoHash;
    }

    public TorrentMeta meta() {
        return this.meta;
    }

    public Deque<Node> peers() {
        return this.peers;
    }

    public void setMeta(TorrentMeta meta) {
        this.meta = meta;
        this.peers.clear();
    }

    @Override
    public String toString() {
        return "Torrent [infoHash=" + this.infoHash + ", peers=" + this.peers + ", meta=" + this.meta + "]";
    }
}
