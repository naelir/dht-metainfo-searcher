package com.naelir.bt;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.naelir.dht.Node;

public class Torrent {
    public static Torrent empty(String hash) {
        Torrent name = new Torrent(hash);
        name.meta = TorrentMeta.EMPTY;
        return name;
    }

    public static Torrent resolved(String hash) {
        Torrent name = new Torrent(hash);
        name.meta = TorrentMeta.RESOLVED;
        return name;
    }

    String infoHash;
    TorrentMeta meta;
    Deque<Node> peers;
    Set<Node> askNodes;

    public Torrent(String infoHash) {
        super();
        this.infoHash = infoHash;
        this.peers = new ArrayDeque<>(20);
        this.askNodes = new CopyOnWriteArraySet<>();
    }

    public Torrent addNodes(Collection<Node> nodes) {
        this.askNodes.addAll(nodes);
        return this;
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

    public Set<Node> getAskNodes() {
        return this.askNodes;
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
