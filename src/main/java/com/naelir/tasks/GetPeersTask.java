package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Converter;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class GetPeersTask implements ITask {
    public static final Logger logger = LogManager.getLogger(GetPeersTask.class);
    private Data data;
    private UtpClient client;
    private AtomicInteger cleanMarker = new AtomicInteger(1);

    public GetPeersTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        int size = this.data.queue.size();
        if (size % 10 == 0) {
            logger.info("{} tasks left to check", size);
        }
        return this.data.queue.isEmpty();
    }

    @Override
    public void run() {
        try {
            for (int step = this.data.config.hashesStep; step > 0; step--) {
                Sample sample = get();
                if (sample == null) {
                    break;
                }
                String infoHash = sample.torrent.infoHash();
                byte[] array = Converter.toArray(infoHash);
                ByteBuffer wrap = ByteBuffer.wrap(array);
                List<Node> closest = this.data.table.closest(sample.byteBuffer(), 10);
                Node selected = select(sample, closest);
                this.client.sendGetPeers(this.data.myself, wrap, selected);
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    Sample get() {
        while (true) {
            Sample pollLast = this.data.queue.poll();
            if (pollLast != null && pollLast.skip) {
                continue;
            }
            if (pollLast == null || pollLast.torrent.meta() == null)
                return pollLast;
        }
    }

    private Node select(Sample sample, List<Node> closest) {
        for (Node node : closest) {
            if (node.applicable() && sample.asked(node) == false) {
                sample.addAsked(node);
                return node;
            }
        }
        return closest.iterator().next();
    }
}
