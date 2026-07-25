package com.naelir.dht;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.IpRangeFilter;
import com.naelir.utp.UtpClient;

public class UdpTorrentResolverTask implements ITask {
    private static final Logger logger = LogManager.getLogger(UdpTorrentResolverTask.class);
    private final Queue<MetaTorrentTask> tasks;
    private UtpClient client;

    public UdpTorrentResolverTask(UtpClient client, Queue<MetaTorrentTask> tasks) {
        this.client = client;
        this.tasks = tasks;
    }

    MetaTorrentTask get() {
        while (true) {
            MetaTorrentTask pollLast = this.tasks.poll();
            if (pollLast == null || pollLast.torrent.meta() == null)
                return pollLast;
        }
    }

    @Override
    public boolean resolved() {
        return this.tasks.isEmpty();
    }

    @Override
    public void run() {
        try {
            int step = 5;
            List<MetaTorrentTask> list = new ArrayList<>(step);
            for (int i = 0; i < step; i++) {
                MetaTorrentTask task = get();
                if (task == null) {
                    continue;
                }
                list.add(task);
            }
            int size = this.tasks.size();
            logger.info("tasks left {}", size);
            for (MetaTorrentTask task : list) {
                if (IpRangeFilter.isAllowed(task.node.ip) == false) {
                    continue;
                }
                String hex = task.torrent.infoHash();
                logger.info("resolving torrent {} from {}, {}", hex, task.node.address(), task.node.port());
                this.client.connectPeer(task.torrent, task.node);
            }
        } catch (Exception e) {
            logger.error("Unexpected error resolving torrent", e);
        }
    }
}
