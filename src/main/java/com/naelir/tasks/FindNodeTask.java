package com.naelir.tasks;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class FindNodeTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FindNodeTask.class);
    private Data data;
    private UtpClient client;

    boolean resolved;
    
    public FindNodeTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.table.nodes().size() >= this.data.arguments.maxNodes || resolved;
        
    }

    @Override
    public void run() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int step = data.arguments.hashesStep;
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
            if (i == 0 && nodes.size() > 0) {
                resolved = true;
            }
            logger.info("findNodes: to {} nodes", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
