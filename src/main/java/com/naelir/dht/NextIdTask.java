package com.naelir.dht;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;

public class NextIdTask implements ITask {
    public static final Logger logger = LogManager.getLogger(NextIdTask.class);

    private Data data;

    public NextIdTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        this.data.nextId();
        String myself = Generator.toHex(data.myself.array());
        logger.warn("next id is {}", myself);
        List<Node> nodes = this.data.table.closest(this.data.myself, 20);
        this.data.table = new RoutingTable();
        nodes.forEach(e -> this.data.table.insert(e));
        this.data.samples.clear();
        this.data.torrents.clear();
        data.fileManager
                .getAll(myself.toLowerCase())
                .forEach(e -> data.torrents.put(e.hash, new Torrent(e.hash, new TorrentMeta(e.hash, e.name))));

    }
}
