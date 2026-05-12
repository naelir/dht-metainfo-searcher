package com.naeir.bt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.naelir.dht.Node;

public class Torrent {
    ByteBuffer infoHash;
    List<Node> peers;

    public Torrent(ByteBuffer infoHash) {
        this(infoHash, new ArrayList<>());
    }

    public Torrent(ByteBuffer infoHash, List<Node> peers) {
        super();
        this.infoHash = infoHash;
        this.peers = peers;
    }

    public List<Node> peers() {
        return this.peers;
    }
}
