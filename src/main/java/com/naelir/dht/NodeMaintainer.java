package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naeir.bt.BitSpaceDivider;
import com.naeir.bt.BtTcpClient;
import com.naeir.bt.Torrent;
import com.naelir.dht.Node.Command;

public class NodeMaintainer implements AutoCloseable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);
    private Data data;
    private UdpClient client;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public NodeMaintainer(UdpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
    }

    public void expire() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            List<Node> expired = new ArrayList<>();
            for (Node node : nodes) {
                Query query1 = node.query(Command.FIND_NODE);
                if (query1 != null && query1.notResponding()) {
                    expired.add(node);
                }
                Query query2 = node.query(Command.PING);
                if (query2 != null && query2.notResponding()) {
                    expired.add(node);
                }
                Query query3 = node.query(Command.SAMPLE);
                if (query3 != null && query3.notResponding()) {
                    expired.add(node);
                }
            }
            this.data.table.removeAll(expired);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void obtainHashes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int size = nodes.size();
            int parts = size % 2 == 1 ? size + 1 : size;
            List<ByteBuffer> divide = BitSpaceDivider.divide(parts);
            int i = 0;
            for (Node node : nodes) {
                Query query = node.query(Command.SAMPLE);
                ByteBuffer range = divide.get(i);
                i++;
                if (query == null || query.shouldRecheck()) {
                    this.client.sendSampleInfohashes(range, node);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void obtainNodes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            if (Config.MAX_NODES > nodes.size()) {
                for (Node node : nodes) {
                    Query query = node.query(Command.FIND_NODE);
                    if (query == null || query.shouldRecheck()) {
                        this.client.sendFindNode(this.data.myself, node);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void obtainTorrents() {
        try {
            Map<ByteBuffer, Torrent> nodes = this.data.torrents;
            for (Torrent torrent : nodes.values()) {
                List<Node> peers = torrent.peers();
                if (peers.isEmpty() == false && torrent.meta() == null) {
                    Node next = peers.iterator().next();
                    BtTcpClient client = new BtTcpClient(torrent.infoHash(), this.data.myself, this.data.torrents);
                    client.connect(InetAddress.getByAddress(next.ip), next.port);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void ping() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            List<Node> expired = new ArrayList<>();
            for (Node node : nodes) {
                Query query = node.query(Command.PING);
                if (query == null || query.shouldRecheck()) {
                    this.client.sendPing(node);
                }
            }
            this.data.table.removeAll(expired);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void start() {
        this.executor.scheduleAtFixedRate(this::obtainNodes, 0, 30, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::ping, 1, 10, TimeUnit.MINUTES);
        this.executor.scheduleAtFixedRate(this::expire, 2, 30, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::obtainHashes, 2, 30, TimeUnit.MINUTES);
        this.executor.scheduleAtFixedRate(this::obtainTorrents, 3, 30, TimeUnit.MINUTES);
    }
}
