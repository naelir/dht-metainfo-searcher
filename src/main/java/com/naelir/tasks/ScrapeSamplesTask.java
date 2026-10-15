package com.naelir.tasks;

import java.util.Set;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;


public class ScrapeSamplesTask implements ITask {

    private Data data;

    public ScrapeSamplesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        Set<String> keySet = data.samples.keySet();
        data.scrapeHashes.addAll(keySet);
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
