package com.naelir.dht;

import java.util.Collection;

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
    
    @Override
    public boolean resolved() {
        return this.data.samples.size() > data.arguments.maxSamples;
    }
}
