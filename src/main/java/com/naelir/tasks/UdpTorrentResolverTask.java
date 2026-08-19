package com.naelir.tasks;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.utp.UtpClient;

public class UdpTorrentResolverTask implements ITask {
    private static final Logger logger = LogManager.getLogger(UdpTorrentResolverTask.class);
    private UtpClient client;
    private Data data;

    public UdpTorrentResolverTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.udptasks.isEmpty();
    }

    @Override
    public void run() {
        try {
            int step = data.arguments.hashesStep;
            List<MetaTorrentTask> list = new ArrayList<>(step);
            for (int i = 0; i < step; i++) {
                MetaTorrentTask task = get();
                if (task == null) {
                    continue;
                }
                list.add(task);
            }
            int size = this.data.udptasks.size();
            logger.info("tasks left {}", size);
            for (MetaTorrentTask task : list) {
                String hex = task.torrent.infoHash();
                InetAddress address = task.node.address();
                Pair<String, String> location = task.node.location();
                logger.info("resolving torrent {} from country {}, {}, {}", hex, location.getRight(), address, task.node.port());
                this.client.connectPeer(task.torrent, task.node);
            }
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
