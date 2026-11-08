package com.naelir.tasks;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class WaitScrapeTask implements ITask {
    
    private Data data;
    int i = 0;
    
    public WaitScrapeTask(Data data) {
        this.data = data;
    }
    
    @Override
    public boolean resolved() {
        return data.forRemoval.size() + data.forUpdate.size() == data.scrapeHashes.size() || i % 10 == 0;
    }

    @Override
    public void run() {//
        i++;
    }

}
