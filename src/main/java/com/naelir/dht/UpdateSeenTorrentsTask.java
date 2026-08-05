package com.naelir.dht;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.NameFilter;

public class UpdateSeenTorrentsTask implements ITask {
    public static final Logger logger = LogManager.getLogger(UpdateSeenTorrentsTask.class);

    private Data data;

    public UpdateSeenTorrentsTask(Data data) {
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
                .filter(e -> e.getValue().getValue() > 0)
                .filter(e -> NameFilter.match(e.getValue().getKey(), true))
                .map(e -> {
                    logger.info("updating seen {}", e.getValue());
                    return e.getKey();
                })
                .toList();
        logger.info("updating {} seen torrents", list.size());
        data.dbRepo.updateMany(list);
        data.forUpdate.clear();
    }
}
