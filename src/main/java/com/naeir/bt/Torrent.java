package com.naeir.bt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.naelir.dht.Node;

public class Torrent {
    ByteBuffer infoHash;
    TorrentMeta meta;
    List<Node> peers;

    public Torrent(ByteBuffer infoHash) {
        this(infoHash, new ArrayList<>());
    }

    public Torrent(ByteBuffer infoHash, List<Node> peers) {
        super();
        this.infoHash = infoHash;
        this.peers = peers;
    }

    public void add(Node node) {
        this.peers.add(node);
    }

    public ByteBuffer infoHash() {
        return this.infoHash;
    }

    public TorrentMeta meta() {
        return this.meta;
    }

    public List<Node> peers() {
        return this.peers;
    }

    public void setMeta(TorrentMeta meta) {
        this.meta = meta;
    }
}
