package com.naelir.dht;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.UtpClient;

public class ScrapeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(ScrapeTask.class);

    private Data data;
    private UtpClient client;
    
    public ScrapeTask(UtpClient client, Data data) {
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
            this.client.scrape(data.scrapeHashes, data.arguments.trackerUrl, data.arguments.trackerPort);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
