package com.naelir.tasks;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;

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
        List<String> list3 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 0 && e.getValue() <= 3)
                .map(e -> e.getKey())
                .toList();
        List<String> list10 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 3 && e.getValue() <= 10)
                .map(e -> e.getKey())
                .toList();
        List<String> list25 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 10 && e.getValue() <= 25)
                .map(e -> e.getKey())
                .toList();
        List<String> list50 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 25 && e.getValue() <= 50)
                .map(e -> e.getKey())
                .toList();
        List<String> list100 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 50 && e.getValue() <= 100)
                .map(e -> e.getKey())
                .toList();
        List<String> list250 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 100 && e.getValue() <= 250)
                .map(e -> e.getKey())
                .toList();
        List<String> list500 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 250 && e.getValue() <= 500)
                .map(e -> e.getKey())
                .toList();
        List<String> list1000 = this.data.forUpdate.stream()
                .filter(e -> e.getValue() > 500)
                .map(e -> e.getKey())
                .toList();
        logger.info("updating {} seen torrents /under 3 peers/", list3.size());
        if (list3.isEmpty() == false) {
            this.data.dbRepo.updateMany(list3, 2);
        }
        logger.info("updating {} seen torrents /under 10 peers/", list10.size());
        if (list10.isEmpty() == false) {
            this.data.dbRepo.updateMany(list10, 7);
        }
        logger.info("updating {} seen torrents /under 20 peers/", list25.size());
        if (list25.isEmpty() == false) {
            this.data.dbRepo.updateMany(list25, 17);
        }
        logger.info("updating {} seen torrents /under 50 peers/", list50.size());
        if (list50.isEmpty() == false) {
            this.data.dbRepo.updateMany(list50, 37);
        }
        logger.info("updating {} seen torrents /under 100 peers/", list100.size());
        if (list100.isEmpty() == false) {
            this.data.dbRepo.updateMany(list100, 75);
        }
        logger.info("updating {} seen torrents /under 250 peers/", list500.size());
        if (list250.isEmpty() == false) {
            this.data.dbRepo.updateMany(list250, 175);
        }
        logger.info("updating {} seen torrents /under 500 peers/", list500.size());
        if (list500.isEmpty() == false) {
            this.data.dbRepo.updateMany(list500, 400);
        }
        logger.info("updating {} seen torrents /over 500 peers/", list1000.size());
        if (list1000.isEmpty() == false) {
            this.data.dbRepo.updateMany(list1000, 600);
        }
        this.data.clear();
    }
}
