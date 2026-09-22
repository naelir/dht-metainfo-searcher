package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
    List<Node> prev;

    public GetPeersTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
        this.prev = new ArrayList<>();
    }

    @Override
    public boolean resolved() {
        int size = this.data.samples.values()
                .stream()
                .filter(s -> s.checked < this.data.arguments.getPeersDepth)
                .toList()
                .size();
        if (size % 10 == 0) {
            logger.info("{} samples left to check", size);
        }
        return this.data.samples.values().stream().allMatch(s -> s.checked >= this.data.arguments.getPeersDepth);
    }

    @Override
    public void run() {
        try {
            int step = this.data.arguments.hashesStep;
            logger.debug("samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
            for (Entry<String, Sample> e : this.data.samples.entrySet()) {
                if (step <= 0) {
                    break;
                }
                Sample sample = e.getValue();
                String infoHash = sample.torrent.infoHash();
                byte[] array = Generator.toArray(infoHash);
                ByteBuffer wrap = ByteBuffer.wrap(array);
                if (sample.checked < this.data.arguments.getPeersDepth) {
                    sample.checked++;
                    if (sample.checked == 1 && this.data.arguments.trackerUrl != null) {
                        this.client.obtainPeers(Set.of(infoHash), this.data.arguments.trackerUrl,
                                this.data.arguments.trackerPort);
                    }
                    if (sample.peers.isEmpty() == false) {
                        logger.debug("samples {} has peers, continue", infoHash);
                        continue;
                    }
                    if (sample.skip) {
                        logger.debug("samples {} is skipped, continue", infoHash);
                        continue;
                    }
                    this.prev.forEach(c -> sample.table.insert(c));
                    List<Node> closest = sample.table.closest(sample.byteBuffer(), 1);
                    this.prev.clear();
                    this.prev.addAll(closest);
                    for (Node node : closest) {
                        ByteBuffer id = node.id();
                        sample.table.remove(id);
                        logger.info("{} {} {} time", infoHash, Generator.toHex(id.array()), sample.checked);
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
