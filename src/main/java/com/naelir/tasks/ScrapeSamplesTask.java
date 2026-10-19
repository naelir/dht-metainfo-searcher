package com.naelir.tasks;

import com.naelir.bt.Entry;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;


public class ScrapeSamplesTask implements ITask {

    private Data data;

    public ScrapeSamplesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        data.samples.keySet()
                .forEach(e -> data.scrapeHashes.add(Entry.lowPeers(e)));
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
