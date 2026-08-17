package com.naelir.tasks;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class CleanAnnounceTask implements ITask {
     
    private Data data;

    public CleanAnnounceTask(Data data) {
        this.data = data;
    }
    
    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        this.data.samples.clear();
    }

}
