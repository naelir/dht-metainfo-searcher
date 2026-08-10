package com.naelir.tasks;

import org.apache.commons.lang3.tuple.Pair;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;


public class CreateScrapeHashesTask implements ITask {

    private Data data;

    public CreateScrapeHashesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        int step = data.arguments.scrapeStep;
        for (Pair<String, String> pair : data.unresolved.values()) {
            data.scrapeHashes.add(pair.getLeft());
            step--;
            if (step <= 0) {
                break;
            }
        }
        
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
