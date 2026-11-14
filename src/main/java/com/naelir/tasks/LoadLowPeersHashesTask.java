package com.naelir.tasks;

import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Converter;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class LoadLowPeersHashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(LoadLowPeersHashesTask.class);

    private Data data;

    public LoadLowPeersHashesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        String substring = Converter.toHex(data.myself.array()).substring(0, 3);
        data.fileManager.readResolved(substring).stream()
                .filter(entry -> entry.name.equals("l"))
                .forEach(e -> data.samples.putIfAbsent(e.hash, new Sample(new Torrent(e.hash), Collections.emptyList(), false)));
        logger.info("Loaded low peers hashes:{} , prefix {}", data.samples.size(), substring);


        int pd = this.data.config.getPeersDepth;
        for (int i = pd; i > 0; i--) {
            for (Sample sample : this.data.samples.values()) {
                this.data.queue.offer(sample);
            }
        }
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
