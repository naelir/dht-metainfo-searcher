package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Entry;
import com.naelir.dht.Converter;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;

public class NextIdTask implements ITask {
    public static final Logger logger = LogManager.getLogger(NextIdTask.class);
    private Data data;
    private ByteBuffer nextId;

    public NextIdTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        this.nextId = this.data.nextId();
        String myself = Converter.toHex(this.nextId.array());
        logger.warn("next id is {}", myself);
        int i = 0;
        int j = 0;
        int k = 0;
        int r = 0;
        for (Sample sample : this.data.samples.values()) {
            String infoHash = sample.torrent.infoHash();
            if (sample.torrent.meta() != null) {
                r++;
            } else if (sample.skip) {
                j++;
                logger.info("skip {}, peers {}", infoHash, sample.peers.size());
            } else if (sample.peers.size() <= 1) {
                i++;
                logger.info("low {}, peers {}", infoHash, sample.peers.size());
                this.data.fileManager.insert(Entry.lowPeers(infoHash));
            } else {
                k++;
                logger.info("not resolved {}, peers {}", infoHash, sample.peers.size());
                this.data.fileManager.insertUnresolved(infoHash);
            }
        }
        logger.warn("samples; low peers {}, crap {}, resolved {}, not {}", i, j, r, k);
        List<Node> nodes = this.data.table.closest(this.nextId, 20);
        this.data.clear();
        for (Node e : nodes) {
            e.queries.clear();
            this.data.table.insert(e);
        }
    }
}
