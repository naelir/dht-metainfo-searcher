package com.naelir.tasks;

import java.util.HashSet;
import java.util.Set;

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
        data.fileManager.writeScrape(data.config.scrapeOut, data.forRemoval);
        data.forRemoval.forEach(e -> data.scrapeHashes.remove(e));
        data.forUpdate.forEach(e -> data.scrapeHashes.remove(e));
        data.forUpdate.clear();
        data.forRemoval.clear();
    }
}
;