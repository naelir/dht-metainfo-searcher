package com.naelir.dht;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NodeMaintainer implements AutoCloseable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);
    private Data data;
    private UdpClient client;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public NodeMaintainer(UdpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
    }

    public void expire() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            List<Node> expired = new ArrayList<>();
            for (Node node : nodes) {
                if (node.query.expired()) {
                    expired.add(node);
                }
            }
            this.data.table.removeAll(expired);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void obtainNodes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            if (Config.MAX_NODES > nodes.size()) {
                for (Node node : nodes) {
                    if (Node.Command.FIND_NODE != node.query().command) {
                        node.query().set(Node.Command.FIND_NODE);
                        this.client.sendFindNode(this.data.myself, node);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void ping() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            if (Config.MAX_NODES > nodes.size()) {
                for (Node node : nodes) {
                    if (node.query().ping()) {
                        node.query().set(Node.Command.PING);
                        this.client.sendPing(node);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void samples() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            for (Node node : nodes) {
                node.query().set(Node.Command.SAMPLE);
                this.client.sendSampleInfohashes(null, node);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void start() {
        this.executor.scheduleAtFixedRate(this::ping, 0, 10, TimeUnit.MINUTES);
        this.executor.scheduleAtFixedRate(this::obtainNodes, 0, 15, TimeUnit.MINUTES);
        this.executor.scheduleAtFixedRate(this::samples, 0, 4, TimeUnit.HOURS);
    }
}
