package com.naelir.tasks;

import com.naelir.bt.Torrent;
import com.naelir.dht.Node;

public final class MetaTorrentTask {
    public final Node node;
    public final Torrent torrent;

    public MetaTorrentTask(Node peers, Torrent torrent) {
        this.node = peers;
        this.torrent = torrent;
    }
}
