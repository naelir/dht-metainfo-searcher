package com.naelir.dht;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.NettyUtpClient;

public class FindSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FindSampleInfohashesTask.class);
    private Data data;
    private NettyUtpClient client;

    public FindSampleInfohashesTask(NettyUtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        boolean allMatch = this.data.table.nodes().stream().allMatch(e -> e.have(Command.SAMPLE));
        if (allMatch) {
            List<Node> delete = this.data.table.nodes()
                    .stream()
                    .filter(e -> e.have(Command.SAMPLE_R) == false)
                    .toList();
            delete.forEach(e -> this.data.table.remove(e.id));
        } else {
            List<Node> list = this.data.table.nodes().stream().filter(e -> e.have(Command.SAMPLE) == false).toList();
            logger.info("nodes to check for samples: {}", list.size());
        }
        return allMatch;
    }

    @Override
    public void run() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int step = 20;
            for (Node node : nodes) {
                if (step < 0) {
                    break;
                }
                if (node.have(Command.SAMPLE) == false) {
                    step--;
                    this.client.sendSampleInfohashes(this.data.myself, this.data.myself, node);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
