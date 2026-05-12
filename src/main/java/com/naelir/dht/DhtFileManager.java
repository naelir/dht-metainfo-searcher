package com.naelir.dht;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DhtFileManager {
    private File file;

    public DhtFileManager() {
        this.file = new File(System.getProperty("user.home"), "dht.info");
    }

    public Info read() {
        if (this.file == null || !this.file.exists())
            return new Info();
        try (FileInputStream fis = new FileInputStream(this.file)) {
            byte[] nodeId = new byte[20];
            int ret = fis.read(nodeId);
            int ipLength = fis.read();
            List<Node> nodes = new LinkedList<>();
            int compactInfoLength = 20 + ipLength + 2;
            byte[] compactInfo = new byte[compactInfoLength];
            while (fis.read(compactInfo) == compactInfoLength) {
                nodes.add(Node.of(ByteBuffer.wrap(compactInfo), ipLength));
            }
            return new Info(nodes, nodeId);
        } catch (IOException e) {
            e.printStackTrace();
            return new Info();
        }
    }

    void save(ByteBuffer myself, List<Node> nodes) {
        try (FileOutputStream fos = new FileOutputStream(this.file);) {
            // First write our own node id - 20 bytes
            fos.write(myself.array());
            // Find the 100 closest nodes
            if (nodes == null || nodes.size() == 0)
                return;
            // Write IP length
            fos.write(nodes.get(0).ip.length);
            // Write their compact info - 26 bytes * number of nodes
            ByteBuffer compact = CompactInfo.compactNodes(nodes);
            fos.write(compact.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Info {
        public List<Node> nodes;
        public ByteBuffer myselfNodeId;

        public Info() {
            this(Collections.emptyList(), Generator.generateRandomID());
        }

        public Info(List<Node> nodes, byte[] nodeId) {
            this.nodes = nodes;
            this.myselfNodeId = ByteBuffer.wrap(nodeId);
        }

        public Info(List<Node> nodes, ByteBuffer nodeId) {
            this.nodes = nodes;
            this.myselfNodeId = nodeId;
        }
    }
}
