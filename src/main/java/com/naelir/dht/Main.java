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
        String host = "localhost";
        int port = 9876;
        ByteBuffer myself = Generator.generateRandomID();
        Data data = new Data(myself);
        OnDataListener crawler = new OnDataListener(data);
        try (UdpClient client = new UdpClient(data, crawler); HouseKeeper keeper = new HouseKeeper(client, data)) {
            // The caller creates and owns the receiver thread.
            Thread receiverThread = new Thread(client, "udp-receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();
            Thread houseKeeperThread = new Thread(keeper, "house-keeper");
            houseKeeperThread.setDaemon(true);
            houseKeeperThread.start();
            InetAddress byName = InetAddress.getByName("127.0.0.1");
            ByteBuffer torrent = ByteBuffer
                    .wrap(UdpClient.hexStringToByteArray("f54bc6f23cc751598486bf6c54ebe3e05d80ec9e"));
//            ByteBuffer tid = UdpClient.getTid(1);
//            GetPeersRequest name = new GetPeersRequest(tid, myself, torrent);
//            client.send(name, byName, 55706);
            Node node = new Node(byName.getAddress(), 55706, null);
            client.sendGetPeers(torrent, node);
//            client.sendSampleInfohashes(myself, node);
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            DhtFileManager fm = new DhtFileManager();
            Collection<Node> values = data.table.nodes();
            List<Node> nodes = new ArrayList<>(values);
            fm.save(myself, nodes);
            fm.save(data.hashes);
        }
    }
}
