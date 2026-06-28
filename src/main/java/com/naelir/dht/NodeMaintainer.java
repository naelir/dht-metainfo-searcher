package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.dht.Node.Command;

public class NodeMaintainer implements AutoCloseable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);
    private Data data;
    private UdpClient client;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public boolean resolved;

    public NodeMaintainer(UdpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
    }

    public void expirePeers() {
        try {
            int i = 0;
            int j = 0;
            int z = 0;
            List<Node> list = new ArrayList<>();

            logger.info("expirePeers run on {} ping tasks", this.data.pingTasks.size());
            List<PingPeersTorrentTask> resolved = new ArrayList<>();
            for (PingPeersTorrentTask task : this.data.pingTasks) {
                for (Node nit : task.getNodes()) {
                    Query query2 = nit.get(Command.FIND_NODE);
                    if (query2 != null) {
                        if (query2.notResponding()) {
                            i++;
                            list.add(nit);
                            logger.info("node {} not reponding", nit.getCounter());
                        } else if (query2.responding()) {
                            j++;
                            this.data.tasks.offer(new MetaTorrentTask(nit, task.torrent));
                            list.add(nit);
                            logger.info("node {} is reponding", nit.getCounter());
                        } else {
                            z++;
                        }
                    }
                }
                task.getNodes().removeAll(list);
                list.clear();
                if (task.getNodes().isEmpty()) {
                    resolved.add(task);
                }
            }
            this.data.pingTasks.removeAll(resolved);
            logger.info("expirePeers removed {} not responding peers, {} tasks found peer, {} not resolved yet", i, j,
                    z);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void expireRoutingTableNodes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            logger.info("expire triggered on {} nodes", nodes.size());
            Set<Node> expired = new HashSet<>();
            for (Node node : nodes) {
                Query query1 = node.get(Command.FIND_NODE);
                if (query1 != null && query1.notResponding()) {
                    expired.add(node);
                }
                Query query4 = node.get(Command.GET_PEER);
                if (query4 != null && query4.notResponding()) {
                    expired.add(node);
                }
                Query query2 = node.get(Command.PING);
                if (query2 != null && query2.notResponding()) {
                    expired.add(node);
                }
                Query query3 = node.get(Command.SAMPLE);
                if (query3 != null && query3.notResponding()) {
                    expired.add(node);
                }
            }
            logger.info("expireRoutingTableNodes will remove {} nodes", expired.size());
            for (Node node : expired) {
                this.data.table.remove(node.id);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void findNodes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int target = 30;
            logger.info("nodes in the routing table {}", nodes.size());
            if (nodes.size() >= data.maxNodes)
                return;
            int i = 0;
            for (Node node : nodes) {
                if (node.get(Command.FIND_NODE) == null) {
                    i++;
                    target--;
                    this.client.sendFindNode(this.data.myself, node);
                }
                if (target < 0) {
                    break;
                }
            }
            logger.info("findNodes: to {} nodes", i);
            if (i == 0) {
                for (Node node : nodes) {
                    node.queryMap.clear();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void findPeers() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            if (nodes.size() < data.maxNodes)
                return;
            logger.info("findPeers: not resolved torrents {}", this.data.torrents.size());
            int i = 5;
            for (Entry<String, Torrent> torrent : this.data.torrents.entrySet()) {
                if (torrent.getValue().meta() == TorrentMeta.EMPTY) {
                    i--;
                    String key = torrent.getKey();
                    byte[] array = Generator.toArray(key);
                    List<Node> closest = this.data.table.closest(ByteBuffer.wrap(array), 8);
                    logger.info("found {} closest to hash {}", closest.size(), key);
                    for (Node node : closest) {
                        this.client.sendGetPeers(ByteBuffer.wrap(array), node);
                    }
                }
                if (i == 0) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void findSampleInfohashes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            if (nodes.size() < data.maxNodes)
                return;
            int target = 20;
            for (Node node : nodes) {
                Query query = node.get(Command.SAMPLE);
                if (query == null) {
                    target--;
                    this.client.sendSampleInfohashes(this.data.myself, node);
                }
                if (target < 0) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void findSampleInfohashesPeers() {
        try {
            logger.info("findSampleInfohashesPeers: not resolved samples {}", this.data.samples.size());
            if (this.data.samples.isEmpty() == false) {
                SampleInfoHashesResponse poll = this.data.samples.poll();
                for (String hash : poll.samples) {
                    Torrent torrent = data.torrents.get(hash);
                    if (torrent == null || torrent.meta() == null) {
                        byte[] array = Generator.toArray(hash);
                        this.client.sendGetPeers(ByteBuffer.wrap(array), poll.request.node);
                    } else {
                        logger.info("hash already resolved {}", hash);

                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void pingPeers() {
        try {
            int limit = 20;
            int i = 0;
            for (PingPeersTorrentTask task : this.data.pingTasks) {
                for (Node node : task.getNodes()) {
                    Query query = node.get(Command.PING);
                    if (limit < 0) {
                        break;
                    }
                    if (query == null) {
                        limit--;
                        i++;
                        this.client.sendPing(node);
                    }
                }
            }
            logger.info("pingPeers is checking {} peers", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    public void findNodeToPeers() {
        try {
            int limit = 20;
            int i = 0;
            for (PingPeersTorrentTask task : this.data.pingTasks) {
                for (Node node : task.getNodes()) {
                    Query query = node.get(Command.FIND_NODE);
                    if (limit < 0) {
                        break;
                    }
                    if (query == null) {
                        limit--;
                        i++;
                        this.client.sendFindNode(data.myself, node);
                    }
                }
            }
            logger.info("findNodeToPeers is checking {} peers", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void pingRoutingTableNodes() {
        try {
            Collection<Node> nodes = this.data.table.nodes();
            int i = 0;
            for (Node node : nodes) {
                Query query = node.get(Command.PING);
                if (query == null || query.shouldRecheck()) {
                    i++;
                    this.client.sendPing(node);
                }
            }
            logger.info("pingRoutingTableNodes is checking {} nodes", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void resolve() {
        if (data.samples.size() == 0) {
            this.resolved = true;
        }
    }

    public void start() {
        this.executor.scheduleAtFixedRate(this::findNodes, 0, 5, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::findSampleInfohashes, 0, 5, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::findSampleInfohashesPeers, 0, 5, TimeUnit.SECONDS);
//      this.executor.scheduleAtFixedRate(this::pingPeers, 0, 5, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::findNodeToPeers, 0, 5, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::expirePeers, 0, 10, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::resolve, 5, 5, TimeUnit.MINUTES);
//      this.executor.scheduleAtFixedRate(this::findPeers, 0, 20, TimeUnit.SECONDS);
//        this.executor.scheduleAtFixedRate(this::pingRoutingTableNodes, 2, 160, TimeUnit.SECONDS);
//        this.executor.scheduleAtFixedRate(this::expireRoutingTableNodes, 2, 30, TimeUnit.MINUTES);
    }
}
