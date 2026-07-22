package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.NettyUtpClient;

public class GetPeersTask implements ITask {
    private static final int QUERY_MAX_NODES = 2;
    private static final int SAMPLE_QUERY_COUNT = 2;
    public static final Logger logger = LogManager.getLogger(GetPeersTask.class);
    private Data data;
    private NettyUtpClient client;

    public GetPeersTask(NettyUtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    List<Node> closest(Sample sample, ByteBuffer wrap) {
        if (sample.isCrap)
            return Collections.emptyList();
        if (sample.checked == 0)
            return List.of(sample.from);
        else if (sample.checked == 1)
            return this.data.table.closest(wrap, 4);
//        else if (sample.checked > 1)
//            return sample.table.closest(wrap, 8);
        else
            return Collections.emptyList();
    }

    @Override
    public boolean resolved() {
        int size = this.data.samples.values().stream().filter(s -> s.checked < SAMPLE_QUERY_COUNT).toList().size();
        logger.info("getPeers: {} samples left to check", size);
        return this.data.samples.values().stream().allMatch(s -> s.checked >= SAMPLE_QUERY_COUNT);
    }

    @Override
    public void run() {
        try {
            int step = 20;
            int i = 0;
            logger.info("getPeers: samples {}, in routing table {}", this.data.samples.size(), this.data.table.size());
            for (Sample sample : this.data.samples.values()) {
                if (step <= 0) {
                    break;
                }
                byte[] array = Generator.toArray(sample.torrent.infoHash());
                ByteBuffer wrap = ByteBuffer.wrap(array);
                if (sample.checked < SAMPLE_QUERY_COUNT) {
                    sample.checked++;
                    if (sample.peers.isEmpty() == false) {
                        logger.info("samples {} has peers, continue", sample.torrent.infoHash());
                        continue;
                    }
                    if (sample.isCrap) {
                        logger.info("samples {} is asian crap, continue", sample.torrent.infoHash());
                        continue;
                    }
                    List<Node> closest = closest(sample, wrap);
                    if (closest.isEmpty()) {
                        continue;
                    }
//                    List<Node> sublist = sublist(closest, 2);
                    logger.info("sample {} sending get peers to {}", sample.torrent.infoHash(), closest.size());
                    for (Node node : closest) {
                        this.client.sendGetPeers(this.data.myself, wrap, node);
                        step--;
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private List<Node> sublist(List<Node> closest, int i) {
        List<Node> list = closest.stream().filter(e -> e.have(Command.GET_PEER) == false).toList();
        int size = list.size();
        int n = Math.min(i, size);
        return list.subList(0, n);
    }
}
