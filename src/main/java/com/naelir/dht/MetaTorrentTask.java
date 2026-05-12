package com.naelir.dht;

import com.naelir.bt.Torrent;

public class MetaTorrentTask {
    private Node node;
    public Torrent torrent;

    public MetaTorrentTask(Node nodes, Torrent torrent) {
        this.node = nodes;
        this.torrent = torrent;
    }

    public Node getNode() {
        return this.node;
    }

    public Torrent getTorrent() {
        return this.torrent;
    }
}
