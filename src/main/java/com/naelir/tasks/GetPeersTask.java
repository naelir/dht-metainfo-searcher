package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.Generator;
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

    List<Node> closest(Sample sample) {
        if (sample.skip)
            return Collections.emptyList();
        else
            return sample.table.closest(sample.byteBuffer(), 1);
    }

    @Override
    public boolean resolved() {
        int size = this.data.samples.values()
                .stream()
                .filter(s -> s.checked < this.data.arguments.getPeersDepth)
                .toList()
                .size();
        logger.debug("getPeers: {} samples left to check", size);
        return this.data.samples.values().stream().allMatch(s -> s.checked >= this.data.arguments.getPeersDepth);
    }

    @Override
    public void run() {
        try {
            int step = data.arguments.hashesStep;
            logger.debug("getPeers: samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
            for (Sample sample : this.data.samples.values()) {
                if (step <= 0) {
                    break;
                }
                String infoHash = sample.torrent.infoHash();
                byte[] array = Generator.toArray(infoHash);
                ByteBuffer wrap = ByteBuffer.wrap(array);
                if (sample.checked < this.data.arguments.getPeersDepth) {
                    sample.checked++;
                    if (sample.peers.isEmpty() == false) {
                        logger.debug("samples {} has peers, continue", infoHash);
                        continue;
                    }
                    if (sample.skip) {
                        logger.debug("samples {} is skipped, continue", infoHash);
                        continue;
                    }
                    List<Node> closest = closest(sample);
                    for (Node node : closest) {
                        ByteBuffer id = node.id();
                        sample.table.remove(id);
                        logger.debug("sample {} getting peers from {} {} time, in table {}", infoHash, Generator.toHex(id.array()), sample.checked, sample.table.size());
                        this.client.sendGetPeers(this.data.myself, wrap, node);
                        step--;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
