package com.naelir.tasks;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.Converter;
import com.naelir.dht.ITask;

public class TrackerReadSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(TrackerReadSampleInfohashesTask.class);

    private Data data;

    public TrackerReadSampleInfohashesTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {

            String hex = Converter.toHex(data.myself.array()).substring(0, 2);
            List<String> searchable = data.fileManager.unresolved(hex);
            for (String key : searchable) {
                String found = data.fileManager.get(key);
                if (found != null) {
                    continue;
                }
                Sample value = new Sample(new Torrent(key), Collections.emptyList(), false);
                data.samples.putIfAbsent(key, value);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
