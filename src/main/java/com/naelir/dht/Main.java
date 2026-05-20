package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Main {
    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.DEBUG);
        ByteBuffer myself = Generator.generateRandomID();
        Data data = new Data(myself);
        OnDataListener crawler = new OnDataListener(data);
        try (
                UdpClient client = new UdpClient(data, crawler);
//                NodeMaintainer keeper = new NodeMaintainer(client, data);
                Scanner scanner = new Scanner(System.in);
        ) {
            client.start();
//            keeper.start();
            InetAddress byName = InetAddress.getByName("localhost");
            ByteBuffer torrent = ByteBuffer
                    .wrap(UdpClient.hexStringToByteArray("ED6D8004994E94A22EDA3EA891E869E5785258E4"));
            Node node = new Node(byName.getAddress(), 54667, null);
            client.sendFindNode(myself, node);
//            client.sendSampleInfohashes(myself, node);
            scanner.nextLine();
            DhtFileManager fm = new DhtFileManager();
            Collection<Node> values = data.table.closest(myself, 100);
            List<Node> nodes = new ArrayList<>(values);
            fm.save(myself, nodes);
//            fm.save(data.hashes);
        }
    }
}
