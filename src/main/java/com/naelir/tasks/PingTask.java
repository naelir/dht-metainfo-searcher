package com.naelir.tasks;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class PingTask implements ITask {
    public static final Logger logger = LogManager.getLogger(PingTask.class);
    private Data data;
    private UtpClient client;

    public PingTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.table.nodes().stream().allMatch(node -> node.have(Command.PING));
    }

    @Override
    public void run() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int step = this.data.config.hashesStep;

            int i = 0;
            for (Node node : nodes) {
                if (step <= 0) {
                    break;
                }
                if (node.have(Command.PING) == false) {
                    i++;
                    step--;
                    this.client.sendPing(this.data.myself, node);
                }
            }
            logger.info("ping: to {} nodes", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
