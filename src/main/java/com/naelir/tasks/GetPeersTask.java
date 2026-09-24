package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;

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
            int step = this.data.config.hashesStep;
            logger.debug("samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
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
                    if (sample.peers.isEmpty() == false) {
                        logger.debug("samples {} has peers, continue", infoHash);
                        continue;
                    }
                    if (sample.skip) {
                        logger.debug("samples {} is skipped, continue", infoHash);
                        continue;
                    }
                    List<Node> closest = sample.table.closest(sample.byteBuffer(), 1);
                    for (Node node : closest) {
                        ByteBuffer id = node.id();
                        sample.table.remove(id);
                        logger.info("{} {} {} time", infoHash, Converter.toHex(id.array()), sample.checked);
                        this.client.sendGetPeers(this.data.myself, wrap, node);
                        step--;
                    }
                } else {
                    sample.table.clear();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
