package com.naelir.tasks;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;


public class CreateScrapeHashesTask implements ITask {

    private Data data;

    public CreateScrapeHashesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        int step = data.config.scrapeStep;
        int min = Math.min(step, data.unresolved.size());
        for (int i = 0; i < min; i++) {
            data.scrapeHashes.add(data.unresolved.poll());
        }
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
