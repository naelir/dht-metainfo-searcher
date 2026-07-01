package com.naelir;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

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
import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.FileManager;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.NodeMaintainer;
import com.naelir.dht.OnDataListener;
import com.naelir.dht.SampleInfoHashesResponse;
import com.naelir.dht.TorrentResolver;
import com.naelir.dht.UdpClient;

public class Application implements Runnable {
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
        return LogManager.getLogger(Application.class);
    }

    private Arguments arguments;
    boolean closed;
    boolean findStart;
    
    public Application(Arguments args) {
        this.arguments = args;
    }

    public void run() {
        try {
            List<ByteBuffer> divide = BitSpaceDivider.divide(arguments.bitspaceParts)
                    .subList(1, arguments.bitspaceParts);
            List<Node> startup = new ArrayList<>();
            for (ByteBuffer udpmyself : divide) {
                String hname = Generator.toHex(udpmyself.array());
                String tcpmyself = Generator.generatePeerID();
                FileManager fm = FileManager.of(hname);
                List<Node> readNodes = List.of();// fm.readDhtCache();
                Set<String> resolved = fm.readMeta();
//                Set<String> unresolved = fm.readHashes();
                Data data = new Data(udpmyself, tcpmyself, fm, arguments.maxNodes);
                OnDataListener crawler = new OnDataListener(data);
                try (
                        UdpClient client = new UdpClient(data, crawler);
                        NodeMaintainer keeper = new NodeMaintainer(client, data);
                        TorrentResolver resolver = new TorrentResolver(data);
                ) {
                    client.start();
                    keeper.start();
                    resolver.start();
//                    unresolved.forEach(e -> data.torrents.put(e, Torrent.empty(e)));
                    logger.info("added {} resolved torrents", resolved.size());
                    resolved.forEach(e -> data.torrents.put(e, Torrent.resolved(e)));
                    startup.forEach(e -> data.table.insert(e));
                    client.explore();
                    while (closed == false) {
                        if (keeper.resolved) {
                            break;
                        }
                        Thread.sleep(2000);
                    }
                    List<Node> nodes = data.table.closest(udpmyself, 50);
                    Set<String> set = new HashSet<>();
                    for (SampleInfoHashesResponse sample : data.samples) {
                        set.addAll(sample.samples);
                    }
                    fm.saveHashes(set);
                    if (readNodes.isEmpty()) {
                        startup.clear();
                        List<Node> subList = nodes.subList(0, 20);
                        for (Node node : startup) {
                            node.queryMap.clear();
                        }
                        startup.addAll(subList);
                    }

                    if (closed) {
                        break;
                    }
                }
            }
        } catch (Exception e2) {
            logger.error(e2.getMessage(), e2);
        }
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments(200, null, 200, "0B851EB851EB851EB851EB851EB851EB851EB84A");// Arguments.parse(args);
        logger.info("Starting with {}", arguments);
        var application = new Application(arguments);
        new Thread(application).start();
        try (
                Scanner name = new Scanner(System.in)
        ) {
            name.nextLine();
        }
        application.closed = true;
    }
}
