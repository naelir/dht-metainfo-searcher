package com.naelir.tasks;

import com.naelir.dht.ITask;

public class WaitScrapeTask implements ITask {
    
    int i = 3;

    @Override
    public boolean resolved() {
        boolean r = i == 0;
        if (r) {
            i = 3;
        }
        return r;
    }

    @Override
    public void run() {//
        i--;
    }

}
