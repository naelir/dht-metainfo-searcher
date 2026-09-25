package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
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
        int size = this.data.samples.values()
                .stream()
                .filter(s -> s.checked < this.data.config.getPeersDepth)
                .toList()
                .size();
        if (size % 10 == 0) {
            logger.info("{} samples left to check", size);
        }
        return this.data.samples.values().stream().allMatch(s -> s.checked >= this.data.config.getPeersDepth);
    }

    @Override
    public void run() {
        try {
            int get = this.cleanMarker.incrementAndGet();
            if (get % 50 == 0) {
                int size = this.data.table.size();
                int ss = this.data.samples.size();
                logger.info("samples {}, in routing table {}", ss, size);
                int t = size / 2;
                List<Node> q = this.data.table.closest(com.naelir.bt.Entry.FIRST_HASH, t);
                for (Node node : q) {
                    this.data.table.remove(node.id());
                }
                logger.info("samples {}, in routing table {}", ss, this.data.table.size());
            }
            int step = this.data.config.hashesStep;
            for (Entry<String, Sample> e : this.data.samples.entrySet()) {
                if (step <= 0) {
                    break;
                }
                Sample sample = e.getValue();
                String infoHash = sample.torrent.infoHash();
                byte[] array = Converter.toArray(infoHash);
                ByteBuffer wrap = ByteBuffer.wrap(array);
                if (sample.checked < this.data.config.getPeersDepth) {
                    sample.checked++;
                    if (sample.peers.size() >= 3) {
                        logger.debug("samples {} has peers, continue", infoHash);
                        continue;
                    }
                    if (sample.skip) {
                        logger.debug("samples {} is skipped, continue", infoHash);
                        continue;
                    }
                    List<Node> closest = this.data.table.closest(sample.byteBuffer(), 5);
                    Node selected = select(sample, closest);
                    ByteBuffer id = selected.id();
                    if (sample.checked == this.data.config.getPeersDepth) {
                        logger.info("{} {} {} time", infoHash, Converter.toHex(id.array()), sample.checked);
                    }
                    this.client.sendGetPeers(this.data.myself, wrap, selected);
                    step--;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Node select(Sample sample, List<Node> closest) {
        for (Node node : closest) {
            if (sample.asked(node) == false) {
                sample.addAsked(node);
                return node;
            }
        }
        return closest.iterator().next();
    }
}
