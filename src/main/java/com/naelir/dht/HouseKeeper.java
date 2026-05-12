package com.naelir.dht;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HouseKeeper implements Runnable, AutoCloseable {
    public static final Logger logger = LogManager.getLogger(HouseKeeper.class);
    private Data data;
    private UdpClient client;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public HouseKeeper(UdpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public void close() throws Exception {
        this.running.set(false);
    }

    @Override
    public void run() {
        this.running.set(true);
        while (this.running.get()) {
            try {
                Collection<Node> nodes = this.data.table.nodes();
                List<RoutingBucket> buckets = this.data.table.buckets();
                List<Node> expired = nodes.stream().filter(e -> e.expired()).toList();
                this.data.table.removeAll(expired);
                List<Node> forGetNodes = nodes.stream().filter(e -> e.asked == false).toList();
                if (nodes.size() < 100) {
                    logger.info("sendFindNode to {} nodes", forGetNodes.size());
                    for (Node node : forGetNodes) {
                        this.client.sendFindNode(this.data.myself, node);
                        node.asked = true;
                    }
                } else {
                    logger.info("nodes list full, not asked for more, but found {} nodes", forGetNodes.size());
                }
                List<Node> forPing = nodes.stream().filter(e -> e.forPing()).toList();
                logger.info("sendSampleInfohashes to {} nodes", forPing.size());
                for (Node node : forPing) {
                    this.client.sendPing(node);
                }
//                for (Node node : forPing) {
//                    this.client.sendSampleInfohashes(this.data.myself, node);
//                }
                logger.info("buckets count {}, nodes {}", buckets.size(), nodes.size());
                TimeUnit.SECONDS.sleep(20);
            } catch (Exception e) {
                if (this.running.get()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
