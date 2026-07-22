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
import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.NodeMaintainer;
import com.naelir.dht.OnDataListener;
import com.naelir.dht.SavedCompactInfo;
import com.naelir.fs.FileDB;
import com.naelir.fs.FileRecord;
import com.naelir.fs.SavedCompactInfoFileManager;
import com.naelir.utp.NettyUtpClient;
import com.naelir.utp.UTPManager;
import com.naelir.utp.UtpDataListener;

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
        Arguments arguments = new Arguments.Builder().continueFrom("1999999999999999999999999999999999999992")
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
            List<FileRecord> all = fm.getAll().stream().filter(e -> "NO_PEERS".equals(e.getName()) == false).toList();
            SavedCompactInfoFileManager peerFm = SavedCompactInfoFileManager.of();
            SavedCompactInfo compactInfo = peerFm.readCompactInfo();
            Data data = new Data(divide, tcpmyself, fm, this.arguments);
            all.forEach(e -> data.torrents.put(e.getId(), Torrent.EMPTY));
            UTPManager manager = new UTPManager();
            UtpDataListener utp = new UtpDataListener(manager);
            OnDataListener udp = new OnDataListener(data);
            try (
                    NettyUtpClient client = new NettyUtpClient(utp, udp, data);
                    BtTcpClient tcp = new BtTcpClient(data);
                    NodeMaintainer keeper = NodeMaintainer.of(data, client, tcp, this.semaphore)
            ) {
                client.start();
                keeper.start();
                List<Node> saved = SavedCompactInfo.nodes(compactInfo);
                client.explore(data.myself, saved);
                this.semaphore.acquire();
                List<Node> nodes = data.table.closest(data.myself, 20);
                peerFm.saveCompactInfo(data.myself, nodes);
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
