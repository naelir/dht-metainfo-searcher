package com.naelir.tasks;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class SampleFindNodeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(SampleFindNodeTask.class);
    private Data data;
    private UtpClient client;

    boolean resolved;
    
    public SampleFindNodeTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        int size = this.data.samples.values()
                .stream()
                .filter(s -> s.checked == 0)
                .toList()
                .size();
        logger.info("samples left for check: {}", size);
        return data.samples.values().stream().allMatch(e -> e.checked > 0);
        
    }

    @Override
    public void run() {
        try {
            int step = data.arguments.hashesStep;
            for (Sample sample : data.samples.values()) {
                if (step < 0) {
                    break;
                }
                if (sample.checked == 0) {
                    step--;
                    sample.checked++;
                    logger.info("sample findNodes for {}", sample.torrent.infoHash());
                    this.client.sendFindNode(this.data.myself, sample.byteBuffer(), sample.from);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
