package com.naelir.dht;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.TorrentMeta;

public class RemoveNoPeersTask implements ITask {
    public static final Logger logger = LogManager.getLogger(RemoveNoPeersTask.class);
    private Data data;

    public RemoveNoPeersTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            int size = this.data.samples.size();
            logger.info("RemoveNoPeersTask run on {} samples", size);
            int i = 0;
            int j = 0;
            for (Sample sample : data.samples.values()) {
                String hash = sample.torrent.infoHash();
                if (sample.isCrap) {
                    i++;
                    this.data.fm.saveMeta(hash, new TorrentMeta("CRAP"));
                }
                if (sample.peers.isEmpty()) {
                    j++;
                    this.data.fm.saveMeta(hash, new TorrentMeta("NAME"));
                }
            }
            this.data.samples.values().removeIf(value -> value.peers.isEmpty());
            logger.info("all samples {}, crap {}, empty{}, remaining {}", size, i, j, size - i - j);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
