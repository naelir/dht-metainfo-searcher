package com.naelir.tasks;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class FindSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FindSampleInfohashesTask.class);
    private Data data;
    private UtpClient client;

    public FindSampleInfohashesTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.table.nodes().stream().allMatch(e -> e.have(Command.SAMPLE));
    }

    @Override
    public void run() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int step = data.arguments.hashesStep;
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
