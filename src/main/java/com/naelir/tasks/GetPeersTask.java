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
                .filter(s -> s.checked < this.data.arguments.queryCount)
                .toList()
                .size();
        logger.info("getPeers: {} samples left to check", size);
        return this.data.samples.values().stream().allMatch(s -> s.checked >= this.data.arguments.queryCount);
    }

    @Override
    public void run() {
        try {
            int step = 10;
            logger.info("getPeers: samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
            for (Sample sample : this.data.samples.values()) {
                if (step <= 0) {
                    break;
                }
                byte[] array = Generator.toArray(sample.torrent.infoHash());
                ByteBuffer wrap = ByteBuffer.wrap(array);
                if (sample.checked < this.data.arguments.queryCount) {
                    if (sample.peers.isEmpty() == false) {
                        sample.checked++;
                        logger.info("samples {} has peers, continue", sample.torrent.infoHash());
                        continue;
                    }
                    if (sample.skip) {
                        sample.checked++;
                        logger.info("samples {} is asian crap, continue", sample.torrent.infoHash());
                        continue;
                    }
                    List<Node> closest = closest(sample);
                    sample.checked++;
                    for (Node node : closest) {
                        ByteBuffer id = node.id();
                        sample.table().remove(id);
                        logger.info("sample {} getting peers from {} {} time", sample.torrent.infoHash(),
                                Generator.toHex(id.array()), sample.checked);
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
