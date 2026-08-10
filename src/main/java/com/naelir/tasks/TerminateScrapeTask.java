package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class TerminateScrapeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(TerminateScrapeTask.class);

    private Data data;

    public TerminateScrapeTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
    }
    
    @Override
    public boolean stop() {
        return data.unresolved.isEmpty() && data.scrapeHashes.isEmpty();
    }
}
