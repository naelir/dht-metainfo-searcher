package com.naelir.tasks;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class FireNextIdTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FireNextIdTask.class);

    private Data data;
    private UtpClient client;

    private boolean trackerScrape;

    public FireNextIdTask(UtpClient client, Data data, boolean trackerScrape) {
        this.data = data;
        this.client = client;
        this.trackerScrape = trackerScrape;
    }
    
    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {

        List<Node> nodes = this.data.table.closest(this.data.myself, 20);
        if (trackerScrape) {
            data.scrapeHashes.clear();
            data.samples.clear();
            data.forUpdate.clear();
            data.forRemoval.clear();
        }
        data.table.clear();
        data.requestsSent.invalidateAll();
        data.tokensSent.invalidateAll();
        data.tokensReceived.invalidateAll();
        data.torrents.clear();
        data.stats.clear();
        data.queue.clear();
        for (Node e : nodes) {
            try {
                this.client.sendFindNode(this.data.myself, this.data.myself, e);
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
            }
        }
    }

}
