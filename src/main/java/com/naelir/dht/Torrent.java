package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Torrent {
    ByteBuffer infoHash;
    ByteBuffer token;
    Node resolvedFrom;
    List<Node> peers;

    public Torrent(ByteBuffer infoHash, ByteBuffer token, Node resolvedFrom) {
        this(infoHash, token, resolvedFrom, new ArrayList<>());
    }

    public Torrent(ByteBuffer infoHash, ByteBuffer token, Node resolvedFrom, List<Node> peers) {
        super();
        this.infoHash = infoHash;
        this.token = token;
        this.resolvedFrom = resolvedFrom;
        this.peers = peers;
    }
}
