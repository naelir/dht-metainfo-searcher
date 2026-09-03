package com.naelir.tasks;

import java.net.InetAddress;

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

    @Override
    public void run() {
        try {
            MetaTorrentTask task = get();
            if (task == null) {
                return;
            }
            int size = this.data.udptasks.size();
            if (size % 10 == 0) {
                logger.debug("tasks left {}", size);
            }
            String hex = task.torrent.infoHash();
            InetAddress address = task.node.address();
            logger.debug("resolving torrent {} from country {}, {}, {}", hex, task.node.location.getRight(), address, task.node.port());
            this.client.connectPeer(task.torrent, task.node);
            
        } catch (Exception e) {
            logger.error("Unexpected error resolving torrent", e);
        }
    }
    

    MetaTorrentTask get() {
        while (true) {
            MetaTorrentTask pollLast = this.data.udptasks.poll();
            if (pollLast == null || pollLast.torrent.meta() == null)
                return pollLast;
        }
    }
}
