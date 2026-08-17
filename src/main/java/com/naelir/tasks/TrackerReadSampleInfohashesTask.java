package com.naelir.tasks;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class TrackerReadSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(TrackerReadSampleInfohashesTask.class);

    private Data data;

    private List<String> searchable;

    public TrackerReadSampleInfohashesTask(Data data) {
        this.data = data;
        this.searchable = data.fileManager.unresolved();
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            int min = Math.min(20, searchable.size());
            var subList = searchable.subList(0, min);
            for (String key : subList) {
                String found = data.fileManager.get(key);
                if (found != null) {
                    continue;
                }
                Sample value = new Sample(new Torrent(key), Collections.emptyList(), false);
                data.samples.putIfAbsent(key, value);
            }
            searchable.removeAll(subList);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
