package com.naelir.tasks;

import com.naelir.dht.ITask;

public class WaitScrapeTask implements ITask {
    
    int i = 3;
    public WaitScrapeTask() {
        // TODO Auto-generated constructor stub
    }
    
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
