package com.naelir.dht;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.NettyUtpClient;

public class PingPeersTask implements ITask {
    public static final Logger logger = LogManager.getLogger(PingPeersTask.class);
    private Data data;
    private NettyUtpClient client;

    public PingPeersTask(NettyUtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.samples.values().stream().allMatch(e -> e.isPing());
    }

    @Override
    public void run() {
        try {
            logger.info("pingPeersTask run on {} samples", this.data.samples.size());
            for (Sample sample : this.data.samples.values()) {
                int step = 20;
                for (Node node : sample.peers) {
                    if (step < 0) {
                        break;
                    }
                    if (node.have(Command.PING) == false) {
                        step--;
                        this.client.sendPing(data.myself, node);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
