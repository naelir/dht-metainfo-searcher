package com.naelir.dht;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DhtFileManager {
    public static final Logger logger = LogManager.getLogger(DhtFileManager.class);
    private File file;
    private Path hashes;

    public DhtFileManager() {
        this.file = new File(System.getProperty("user.home"), "dht.info");
        this.hashes = Paths.get(System.getProperty("user.home"), "hashes.info");
    }

    Info read() {
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
            fos.write(myself.array());
            if (nodes == null || nodes.size() == 0)
                return;
            fos.write(nodes.get(0).ip.length);
            ByteBuffer compact = CompactInfo.compactNodes(nodes);
            fos.write(compact.array());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    void save(Set<String> hashes) {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(this.hashes, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (String string : hashes) {
                bufferedWriter.append(string);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class Info {
        public List<Node> nodes;
        public ByteBuffer myself;

        public Info() {
            this(Collections.emptyList(), Generator.generateRandomID());
        }

        public Info(List<Node> nodes, byte[] nodeId) {
            this.nodes = nodes;
            this.myself = ByteBuffer.wrap(nodeId);
        }

        public Info(List<Node> nodes, ByteBuffer nodeId) {
            this.nodes = nodes;
            this.myself = nodeId;
        }
    }
}
