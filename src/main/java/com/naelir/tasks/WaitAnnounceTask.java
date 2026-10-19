package com.naelir.tasks;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class WaitAnnounceTask implements ITask {
     
    private Data data;
    int i = 7;

    public WaitAnnounceTask(Data data) {
        this.data = data;
    }
    
    @Override
    public boolean resolved() {
        return i <= 0;
    }

    @Override
    public void run() {//
        i--;
    }

}
