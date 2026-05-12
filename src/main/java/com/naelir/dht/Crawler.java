package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Crawler {
    public static final Logger logger = LogManager.getLogger(Crawler.class);

    private static List<Node> contactPoints() throws UnknownHostException {
        byte[] byName1 = InetAddress.getByName("router.bittorrent.com").getAddress();
        byte[] byName2 = InetAddress.getByName("dht.transmissionbt.com").getAddress();
        byte[] byName3 = InetAddress.getByName("router.utorrent.com").getAddress();
        return List.of(new Node(byName1, 6881, null), new Node(byName2, 6881, null), new Node(byName3, 6881, null));
    }

    static ByteBuffer getTid(int id) {
        id = id % Config.MAX_MESSAGE_ID;
        byte[] bytes = { (byte) (id & 0xFF), (byte) ((id & 0xFF00) >>> 8) };
        return ByteBuffer.wrap(bytes);
    }

    private ByteBuffer myself;
    private Data data;
    private UdpClient client;

    public Crawler(UdpClient connection) {
        this.client = connection;
        this.myself = Generator.generateRandomID();
        this.data = new Data(this.myself);
    }

    public void seed() throws Exception {
        List<Node> contactPoints = contactPoints();
        ByteBuffer tid = getTid(1);
        for (Node node : contactPoints) {
            CommandId commandId = new CommandId(tid, node.ip, node.port);
            FindNodeRequest initial = new FindNodeRequest(tid, this.myself, this.myself);
            byte[] bytes = BEncoder.encode(initial);
            this.client.send(bytes, node.address(), node.port);
            this.data.commandsSent.put(commandId, initial);
        }
    }
}
