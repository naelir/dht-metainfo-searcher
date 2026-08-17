package com.naelir.tasks;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            Set<String> set = new HashSet<String>(data.samples.keySet());
            if (set.isEmpty()) {
                logger.info("No samples to find peers for");
                return;
            }
            this.client.obtainPeers(set, data.arguments.trackerUrl, data.arguments.trackerPort);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
