package com.naelir.dht;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;

public class TorrentResolver implements Runnable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(TorrentResolver.class);
    private final Data data;
    private volatile boolean running = true;

    public TorrentResolver(Data data) {
        this.data = data;
    }

    @Override
    public void close() throws Exception {
        this.running = false;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                MetaTorrentTask task = this.data.tasks.poll();
                if (task == null) {
                    Thread.sleep(100);
                    continue;
                }
                String hex = task.torrent.infoHash();
                if (this.data.torrents.containsKey(hex) && this.data.torrents.get(hex).meta() != null) {
                    logger.info("torrent {} already resolved, skipping.", hex);
                    continue;
                }

                Node node = task.getNode();
                logger.info("will try to resolve torrent {} from {}, {}", hex, node.address(), node.port());
                BtTcpClient client = new BtTcpClient(task.torrent, node, this.data);
                client.connect();

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
