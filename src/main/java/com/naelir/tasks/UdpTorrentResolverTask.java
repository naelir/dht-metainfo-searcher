package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.utp.UtpClient;

public class UdpTorrentResolverTask implements Runnable {
    private static final Logger logger = LogManager.getLogger(UdpTorrentResolverTask.class);
    private UtpClient client;
    private Data data;

    public UdpTorrentResolverTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    MetaTorrentTask get() {
        while (true) {
            MetaTorrentTask pollLast = this.data.udptasks.poll();
            if (pollLast == null || pollLast.torrent.meta() == null)
                return pollLast;
        }
    }

    @Override
    public void run() {
        try {
            MetaTorrentTask task = get();
            if (task == null)
                return;
            int size = this.data.udptasks.size();
            if (size > 0 && size % 10 == 0) {
                logger.info("tasks left {}", size);
            }
            String hex = task.torrent.infoHash();
            if (task.node.location != null) {
                logger.info("{} from {}", hex, task.node.location.getRight());
            }
            this.client.connectPeer(task.torrent, task.node);
        } catch (Exception e) {
            logger.error("Unexpected error resolving torrent", e);
        }
    }
}
