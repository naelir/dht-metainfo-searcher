package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class PopulateSampleTableTask implements ITask {
    public static final Logger logger = LogManager.getLogger(PopulateSampleTableTask.class);

    private Data data;
    private UtpClient client;

    public PopulateSampleTableTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }
    
    @Override
    public void run() {
        try {
            int step = data.arguments.hashesStep;
            for (Sample sample : this.data.samples.values()) {
                if (step <= 0) {
                    break;
                }
                if (sample.checked >= 1) {
                    continue;
                }
                sample.checked++;
                ByteBuffer hash = sample.byteBuffer();
                List<Node> closest = sample.table.closest(hash, 1);
                for (Node node : closest) {
                    logger.info("sending find_node for routing table on hash {}", sample.torrent.infoHash());
                    this.client.sendFindNode(data.myself, hash, node);
                    step--;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean resolved() {
        return this.data.samples.values().stream().allMatch(s -> s.checked >= 1);
    }
}
