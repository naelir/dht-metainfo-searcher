package com.naelir.tasks;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Entry;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
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
        return data.udptasks.isEmpty();
    }

    @Override
    public void run() {
        try {
            Set<Entry> set = data.samples.keySet()
                    .stream()
                    .map(e -> Entry.lowPeers(e))
                    .collect(Collectors.toCollection(HashSet::new));
            if (set.isEmpty()) {
                logger.info("No samples to find peers for");
                return;
            }
            this.client.obtainPeers(set, data.config.trackerUrl, data.config.trackerPort);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
