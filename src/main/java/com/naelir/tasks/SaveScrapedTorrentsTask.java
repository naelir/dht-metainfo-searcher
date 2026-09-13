package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class SaveScrapedTorrentsTask implements ITask {
    public static final Logger logger = LogManager.getLogger(SaveScrapedTorrentsTask.class);

    private Data data;

    public SaveScrapedTorrentsTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        data.fileManager.writeScrape(data.arguments.scrapeOut, data.forUpdate);
        data.forUpdate.forEach(e -> data.scrapeHashes.remove(e.getKey()));
        data.forUpdate.clear();
    }
}
