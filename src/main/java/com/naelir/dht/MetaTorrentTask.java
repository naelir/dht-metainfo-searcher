package com.naelir.dht;

import com.naelir.bt.Torrent;

public class MetaTorrentTask {
    public final Node node;
    public final Torrent torrent;

    public MetaTorrentTask(Node peers, Torrent torrent) {
        this.node = peers;
        this.torrent = torrent;
    }
}
