package com.naelir.dht;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.UtpClient;

public class TrackerFindPeersTask implements ITask {
    public static final Logger logger = LogManager.getLogger(TrackerFindPeersTask.class);

    private Data data;
    private UtpClient client;
    
    public TrackerFindPeersTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            logger.info("getPeers: samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
            Set<String> collect = data.samples.values().stream()
                    .map(e -> e.torrent.infoHash())
                    .collect(Collectors.toSet());
            this.client.scrape(collect, data.arguments.trackerUrl, data.arguments.trackerPort);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
