package com.naelir.dht;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.NettyUtpClient;

public class FindNodeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FindNodeTask.class);
    private Data data;
    private NettyUtpClient client;

    public FindNodeTask(NettyUtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.table.nodes().size() >= 400;
    }

    @Override
    public void run() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int step = 20;
            logger.info("nodes in the routing table {}", nodes.size());
            int i = 0;
            for (Node node : nodes) {
                if (step < 0) {
                    break;
                }
                if (node.have(Command.FIND_NODE) == false) {
                    i++;
                    step--;
                    this.client.sendFindNode(this.data.myself, this.data.myself, node);
                }
            }
            logger.info("findNodes: to {} nodes", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
