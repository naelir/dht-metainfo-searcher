package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;
import com.naelir.dht.Data;

public class TcpTorrentResolverTask implements Runnable {
    private static final Logger logger = LogManager.getLogger(TcpTorrentResolverTask.class);
    private BtTcpClient client;
    private Data data;

    public TcpTorrentResolverTask(BtTcpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            if (data.tcptasks.isEmpty()) {
                return;
            }
            MetaTorrentTask task = get();
            if (task == null) {
                return;
            }
            int size = this.data.tcptasks.size();
            if (size % 10 == 0 && size > 0) {
                logger.info("tasks left {}", size);
            }
            String hex = task.torrent.infoHash();
            logger.info("resolving torrent {} from {}, {}", hex, task.node.address(), task.node.port());
            this.client.connect(task.torrent, task.node);
            
        } catch (Exception e) {
            logger.error("Unexpected error resolving torrent", e);
        }
    }

    MetaTorrentTask get() {
        while (true) {
            MetaTorrentTask pollLast = this.data.tcptasks.pollLast();
            if (pollLast == null || pollLast.torrent.meta() == null)
                return pollLast;
        }
    }
}
