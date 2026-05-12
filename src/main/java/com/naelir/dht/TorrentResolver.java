package com.naelir.dht;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;
import com.naelir.bt.TorrentMeta;

public class TorrentResolver implements Runnable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(TorrentResolver.class);
    private final Data data;
    private volatile boolean running = true;
    List<Node> black;

    public TorrentResolver(Data data) {
        this.data = data;
        this.black = new ArrayList<>();
    }

    @Override
    public void close() throws Exception {
        this.running = false;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                int size = this.data.tasks.size();
                MetaTorrentTask task = this.data.tasks.poll();
                if (task == null) {
                    Thread.sleep(100);
                    continue;
                }
                String hex = task.torrent.infoHash();
                if (this.data.torrents.containsKey(hex) && this.data.torrents.get(hex).meta() == TorrentMeta.RESOLVED) {
                    logger.info("torrent {} already resolved, skipping.", hex);
                    continue;
                }
                if (this.black.contains(task.getNode())) {
                    continue;
                }
                this.data.unresolved.add(hex);
                Node node = task.getNode();
                logger.info("will try to resolve torrent {} from {}, {}", hex, node.address(), node.port());
                BtTcpClient client = new BtTcpClient(task.torrent, node, this.data);
                client.connect();
                if (task.torrent.meta() == TorrentMeta.EMPTY || task.torrent.meta() == TorrentMeta.SCAM) {
                    this.black.add(node);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("TorrentResolver interrupted, stopping.");
                break;
            } catch (Exception e) {
                logger.error("Unexpected error resolving torrent", e);
            }
        }
        logger.info("TorrentResolver stopped.");
    }

    public void start() {
        new Thread(this, "bt-initiator").start();
    }
}
