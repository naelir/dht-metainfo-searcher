package com.naelir;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.naelir.bt.BitSpaceDivider;
import com.naelir.bt.BtTcpClient;
import com.naelir.bt.Entry;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.NodeMaintainer;
import com.naelir.dht.UdpOnDataListener;
import com.naelir.dht.SavedCompactInfo;
import com.naelir.fs.FileDB;
import com.naelir.fs.SavedCompactInfoFileManager;
import com.naelir.tracker.TrackerOnDataListener;
import com.naelir.utp.UtpClient;
import com.naelir.utp.UTPManager;
import com.naelir.utp.UtpOnDataListener;

public final class DhtApplication implements Runnable {
    static final Logger logger = logger();

    static Logger logger() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.INFO);
        LayoutComponentBuilder layout = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n");
        AppenderComponentBuilder console = builder.newAppender("Console", "CONSOLE").add(layout);
        AppenderComponentBuilder file = builder.newAppender("LogFile", "FILE")
                .addAttribute("fileName", "dht-logs.log")
                .add(layout);
        builder.add(console);
        builder.add(file);
        builder.add(builder.newRootLogger(Level.INFO)
                .add(builder.newAppenderRef("Console"))
                .add(builder.newAppenderRef("LogFile")));
        Configurator.initialize(builder.build());
        return LogManager.getLogger(DhtApplication.class);
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments.Builder()
                .continueFrom("1C28F5C28F5C28F5C28F5C28F5C28F5C28F5C288")
                .bitspaceParts(100)
                .build();
        logger.info("Starting with {}", arguments);
        var application = new DhtApplication(arguments);
        new Thread(application, "dht-metainfo").start();
        try (
                Scanner name = new Scanner(System.in)
        ) {
            name.nextLine();
        }
        application.stop();
    }

    private Arguments arguments;
    private final Semaphore semaphore = new Semaphore(0);

    public DhtApplication(Arguments args) {
        this.arguments = args;
    }

    Queue<ByteBuffer> divide(BigInteger from) {
        List<ByteBuffer> divide = BitSpaceDivider.divide(this.arguments.bitspaceParts);
        Queue<ByteBuffer> list = new LinkedList<>();
        for (ByteBuffer udpmyself : divide) {
            String hname = Generator.toHex(udpmyself.array());
            if (from.compareTo(new BigInteger(1, udpmyself.array())) > 0) {
                logger.info("skipping {}", hname);
                continue;
            }
            list.add(udpmyself);
        }
        return list;
    }

    @Override
    public void run() {
        try {
            BigInteger from = this.arguments.continueFrom != null
                    ? new BigInteger(1, Generator.toArray(this.arguments.continueFrom))
                    : BigInteger.ZERO;
            Queue<ByteBuffer> divide = divide(from);
            String tcpmyself = Generator.generatePeerID();
            FileDB fm = FileDB.of();
            
            SavedCompactInfoFileManager peersFm = SavedCompactInfoFileManager.of();
            SavedCompactInfo compactInfo = peersFm.readCompactInfo();
            Data data = new Data(divide, tcpmyself, fm, this.arguments);
            String myself = Generator.toHex(data.myself.array());
            fm.getAll(myself.toLowerCase()).forEach(e -> data.torrents.put(e.hash, new Torrent(e.hash, new TorrentMeta(e.hash, e.name))));
            UTPManager manager = new UTPManager();
            UtpOnDataListener utp = new UtpOnDataListener(manager);
            UdpOnDataListener udp = new UdpOnDataListener(data);
            TrackerOnDataListener tr = new TrackerOnDataListener(data);
            try (
                    UtpClient utpClient = new UtpClient(utp, udp, tr, data);
                    BtTcpClient tcpClient = new BtTcpClient(data);
                    NodeMaintainer maintainer = NodeMaintainer.of(data, utpClient, tcpClient, this.semaphore)
            ) {
                utpClient.start();
                maintainer.start();
                List<Node> saved = SavedCompactInfo.nodes(compactInfo);
                utpClient.explore(data.myself, saved);
                this.semaphore.acquire();
                List<Node> nodes = data.table.closest(data.myself, 20);
                peersFm.saveCompactInfo(data.myself, nodes);
                logger.info("stopped with {}", Generator.toHex(data.myself.array()));
            }
        } catch (Exception e2) {
            logger.error(e2.getMessage(), e2);
        }
    }

    void stop() {
        this.semaphore.release();
    }
}
