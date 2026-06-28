package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.naelir.bt.BtTcpClient;
import com.naelir.bt.Torrent;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.DEBUG);
//        loggerConfig();
        ByteBuffer udpmyself = Generator.generateRandomID();
        String tcpmyself = Generator.generatePeerID();
        int serverPort = 6888;
        String tmp = Generator.toHex(udpmyself.array()).concat(".tmp");
        FileManager fm = FileManager.of(tmp);
        Data data = new Data(udpmyself, tcpmyself, fm, 100);
        OnDataListener crawler = new OnDataListener(data);
//        DhtInfo info = fm.readDhtCache();
//        List<Node> readNodes = info.nodes;
//        Set<String> existingHashes = fm.readMeta();
        try (
                UdpClient client = new UdpClient(data, crawler);
//                NodeMaintainer keeper = new NodeMaintainer(client, data);
//                TorrentResolver resolver = new TorrentResolver(data);
        ) {
            client.start();
//            name.start();
//            keeper.start();
//            resolver.start();
//            existingHashes.forEach(e -> data.torrents.put(e, Torrent.empty(e)));
//          readNodes.forEach(e -> data.table.insert(e));
//            logger.info("added {} nodes", readNodes.size());
//            String tt = "69B1D9007192BDA3F616C1C3C481C95F7C5F8300";
//            byte[] btes = Generator.toArray(tt);
//            int port = 20004;
            InetAddress addr = InetAddress.getByName("localhost");
            // transmission
//            String tt = "4604ff1bd54982443fd824b11e864a01b7d99b09";
//            byte[] btes = Generator.toArray(tt);
//            int port = 23232;
            // deluge
//            byte[] btes = Generator.toArray("fa364df414550722d55d8f03071faa5fb782a3af");
//            int port = 6881;
            // qb fc43a8dbe2c723ffd857d13f4cd513a93f251c2e
//            String tt = "a25f87ceb33224161a4df4fa6c679d3f496cbe6f";
//            byte[] btes = Generator.toArray(tt);
//            int port = 56514;
            // bitcomet fc43a8dbe2c723ffd857d13f4cd513a93f251c2e
            String tt = "d36e7acc0ac863e6e4ae984bfe20c4099e1d82fc";
            byte[] btes = Generator.toArray(tt);
            int port = 6882;
            // utorrent
//            String tt = "FC43A8DBE2C723FFD857D13F4CD513A93F251C2E";
////            byte[] btes = Generator.toArray(tt);
//            int port = 60113;
//            ByteBuffer torrent = ByteBuffer.wrap(btes);
            Node node = new Node(addr.getAddress(), port, Generator.generateRandomID());
//            String hex = Generator.toHex(btes);
//            data.torrents.put(hex, new Torrent(hex));
//            data.table.insert(node);
            Torrent torrent = new Torrent(tt);
//            client.sendGetPeers(torrent, node);
//            client.sendFindNode(udpmyself, node);
//            client.sendSampleInfohashes(udpmyself, node);
//            client.sendPing(node);
//            ByteBuffer tcpmyself = Generator.generatePeerID();
//            data.torrents.put(torrent.infoHash(), torrent);
//            Torrent torrent = new Torrent(tt);
//            MetaTorrentTask task = new MetaTorrentTask(node, torrent);
            BtTcpClient btTcpClient = new BtTcpClient(torrent, node, data);
            btTcpClient.connect();
//            client.explore();
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
//            Collection<Node> values = data.table.closest(udpmyself, 100);
            Set<String> values = data.table.nodes()
                    .stream()
                    .filter(e -> e.id != null)
                    .map(e -> Generator.toHex(e.id.array()))
                    .collect(Collectors.toSet());
//            Path
//            List<Node> nodes = new ArrayList<>(values);
//            fm.saveMeta(data.torrents.values());
            Path any = Paths.get(System.getProperty("user.home"), "close-nodes.info");
//            fm.saveDhtNodes(udpmyself, nodes);
            fm.saveHashesTo(values, any);
        }
    }
}
