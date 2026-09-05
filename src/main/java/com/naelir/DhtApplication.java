package com.naelir;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
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
import com.naelir.dht.Data;
import com.naelir.dht.DhtResponseResolver;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.SavedCompactInfo;
import com.naelir.dht.UdpOnDataListener;
import com.naelir.fs.FileDB;
import com.naelir.fs.FileLocationDb;
import com.naelir.fs.ILocationDb;
import com.naelir.fs.SavedCompactInfoFileManager;
import com.naelir.fs.UnresolvedFileManager;
import com.naelir.tasks.NodeMaintainer;
import com.naelir.tasks.UdpTorrentResolverTask;
import com.naelir.tracker.TrackerOnDataListener;
import com.naelir.tracker.TrackerUdpManager;
import com.naelir.utp.InboundHandler;
import com.naelir.utp.UTPManager;
import com.naelir.utp.UtpClient;
import com.naelir.utp.UtpOnDataListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public final class DhtApplication implements Runnable {
    static final Logger logger = LogManager.getLogger(DhtApplication.class);

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        logger.info("Starting with {}", arguments);

        new DhtApplication(arguments).run();
    }

    private final Arguments arguments;
    private final Semaphore semaphore;

    public DhtApplication(Arguments args) {
        this.arguments = args;
        this.semaphore = new Semaphore(0);
    }

    @Override
    public void run() {
        try {
            BigInteger to = this.arguments.to != null
                    ? new BigInteger(1, Generator.toArray(this.arguments.to))
                    : BigInteger.ONE.shiftLeft(160).subtract(BigInteger.ONE);
            BigInteger from = this.arguments.from != null
                    ? new BigInteger(1, Generator.toArray(this.arguments.from))
                    : BigInteger.ZERO;
            Queue<ByteBuffer> udpmyselfs = divide(from, to);
            String tcpmyself = Generator.generatePeerID();
            FileDB fileDB = FileDB.of();
            
            SavedCompactInfoFileManager peersFm = SavedCompactInfoFileManager.of();
            SavedCompactInfo compactInfo = peersFm.readCompactInfo();
            ILocationDb locationDb = FileLocationDb.INSTANCE;
            Data data = new Data(udpmyselfs, tcpmyself, fileDB, locationDb, this.arguments);

            if (arguments.scrape) {
                UnresolvedFileManager ufm = UnresolvedFileManager.of();
                List<Pair<String, String>> all = ufm.getAll();
                data.unresolved.addAll(all);
                logger.info("loaded {} unresolved", all.size());
            }

            UTPManager utpManager = new UTPManager();
            TrackerUdpManager trackerUdpManager = new TrackerUdpManager(data);
            UtpOnDataListener utp = new UtpOnDataListener(utpManager);
            DhtResponseResolver dht = new DhtResponseResolver(data);
            UdpOnDataListener udp = new UdpOnDataListener(dht);
            TrackerOnDataListener trackerUdp = new TrackerOnDataListener(trackerUdpManager);
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("scheduler"));
            IoHandlerFactory newFactory = NioIoHandler.newFactory();
            DefaultThreadFactory threadFactory = new DefaultThreadFactory("utp-client");
            MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(1, threadFactory, newFactory);
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, false)
                    .handler(new InboundHandler(utp, udp, trackerUdp));
            // Bind to any available local port
            var channel = bootstrap.bind(0).sync().channel();
            
            logger.info("udp channel bound to {}", channel.localAddress());
            
            try (
                    UtpClient utpClient = new UtpClient(channel, data, utpManager, trackerUdpManager);
                    BtTcpClient tcpClient = new BtTcpClient(data);
            ) {
                NodeMaintainer maintainer = NodeMaintainer.of(data, utpClient, tcpClient);
                UdpTorrentResolverTask resolverTask = new UdpTorrentResolverTask(utpClient, data);
                
                executor.scheduleAtFixedRate(utpClient::tick, UtpClient.TICK_INTERVAL_MS, UtpClient.TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
                executor.scheduleAtFixedRate(maintainer, 0, arguments.scheduleInterval, TimeUnit.SECONDS);
                executor.scheduleAtFixedRate(resolverTask, 0, arguments.resolverMillis, TimeUnit.MILLISECONDS);
                List<Node> saved = SavedCompactInfo.nodes(compactInfo);
                if (arguments.scrape == false) {
                    utpClient.explore(data.myself, saved);
                }

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Received SIGTERM, shutting down");
                    semaphore.release();

                    if (arguments.scrape == false) {
                        List<Node> nodes = data.table.closest(data.myself, 20);
                        peersFm.saveCompactInfo(data.myself, nodes);
                        logger.info("stopped with {}", Generator.toHex(data.myself.array()));
                    }
                }, "dht-shutdown"));
                this.semaphore.acquire();

            } finally {
                fileDB.close();
                executor.shutdown();
                group.shutdownGracefully();
            }
        } catch (Exception e2) {
            logger.error(e2.getMessage(), e2);
        }
    }

    Queue<ByteBuffer> divide(BigInteger from, BigInteger to) {
        List<ByteBuffer> divide = BitSpaceDivider.divide(this.arguments.bitspaceParts);
        return divide.stream()
                .filter(
                        e -> from.compareTo(new BigInteger(1, e.array())) <= 0 && to.compareTo(new BigInteger(1, e.array())) >= 0
                 )
                .collect(Collectors.toCollection(LinkedList::new));
    }
}