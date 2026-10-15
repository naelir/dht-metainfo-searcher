package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Entry;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class FilterSamplesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FilterSamplesTask.class);
    private Data data;

    public FilterSamplesTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        logger.info("samples before {}", data.samples.size());
        this.data.forRemoval.stream()
                .forEach(e -> {
                    data.samples.remove(e.getKey());
                    data.fileManager.insert(Entry.lowPeers(e.getKey()));
                });
        this.data.forRemoval.clear();
        this.data.scrapeHashes.clear();
        logger.info("samples after {}", data.samples.size());

    }
}
