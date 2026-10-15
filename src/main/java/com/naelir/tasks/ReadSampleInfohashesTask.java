package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.Converter;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;

public class ReadSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(ReadSampleInfohashesTask.class);
    private Data data;

    public ReadSampleInfohashesTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            List<String> searchable = data.fileManager.unresolved(data.config.unresolvedFile);
            for (String key : searchable) {
                Sample value = new Sample(new Torrent(key), Collections.emptyList(), false);
                data.samples.putIfAbsent(key, value);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
