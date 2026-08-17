package com.naelir.tasks;

import java.util.List;

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
        int min = Math.min(step, data.unresolved.size());
        List<Pair<String, String>> subList = data.unresolved.subList(0, min);
        subList.forEach(pair -> data.scrapeHashes.add(pair.getLeft()));
        data.unresolved.removeAll(subList);
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
