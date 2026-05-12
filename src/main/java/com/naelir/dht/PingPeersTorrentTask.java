package com.naelir.dht;

import java.util.ArrayList;
import java.util.List;

import com.naelir.bt.Torrent;

public class PingPeersTorrentTask {
    private List<Node> nodes;
    public Torrent torrent;

    public PingPeersTorrentTask(List<Node> nodes, Torrent torrent) {
        this.nodes = new ArrayList<>(nodes);
        this.torrent = torrent;
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public Torrent getTorrent() {
        return this.torrent;
    }
}
