package com.naelir.dht;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;

public class TcpTorrentResolverTask implements ITask {
    private static final Logger logger = LogManager.getLogger(TcpTorrentResolverTask.class);
    private final Deque<MetaTorrentTask> tasks;
    private BtTcpClient client;

    public TcpTorrentResolverTask(BtTcpClient client, Deque<MetaTorrentTask> tasks) {
        this.client = client;
        this.tasks = tasks;
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
                MetaTorrentTask task = this.tasks.pop();
                if (task == null) {
                    continue;
                }
                if (task.torrent.meta() != null) {
                    logger.info("will not get, torrent {} resolved", task.torrent.meta().getName());
                    continue;
                }
                list.add(task);
            }
            int size = this.tasks.size();
            logger.info("tasks left {}", size);
            for (MetaTorrentTask task : list) {
                String hex = task.torrent.infoHash();
                logger.info("resolving torrent {} from {}, {}", hex, task.node.address(), task.node.port());
                this.client.connect(task.torrent, task.node);
            }
        } catch (Exception e) {
            logger.error("Unexpected error resolving torrent", e);
        }
    }
}
