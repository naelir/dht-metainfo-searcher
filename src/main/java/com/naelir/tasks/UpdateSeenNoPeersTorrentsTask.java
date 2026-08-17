package com.naelir.tasks;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class UpdateSeenNoPeersTorrentsTask implements ITask {
    public static final Logger logger = LogManager.getLogger(UpdateSeenNoPeersTorrentsTask.class);

    private Data data;

    public UpdateSeenNoPeersTorrentsTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        List<String> list = this.data.forUpdate
                .stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> {
                    return e.getKey();
                })
                .toList();
        logger.info("updating {} seen torrents", list.size());
        data.dbRepo.updateMany(list);
        data.forUpdate.clear();
        data.scrapeHashes.clear();
    }
}
