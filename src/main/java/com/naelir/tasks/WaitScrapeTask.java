package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class WaitScrapeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(WaitScrapeTask.class);

    private Data data;
    
    public WaitScrapeTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return data.forUpdate.size() == data.scrapeHashes.size();
    }

    @Override
    public void run() {
    }

}
