package com.naelir.tasks;

import com.naelir.dht.ITask;

public class WaitScrapeTask implements ITask {
    
    int counter = 0;

    @Override
    public boolean resolved() {
        boolean b = counter >= 1;;
        if (b) {
            counter = 0;
        }
        return b;
    }

    @Override
    public void run() {
        counter++;
    }

}
