package com.naelir.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Node;
import com.naelir.dht.SavedCompactInfo;

public class SavedCompactInfoFileManager {

    public static final Logger logger = LogManager.getLogger(SavedCompactInfoFileManager.class);
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");

    public static SavedCompactInfoFileManager of() throws IOException {
        Files.createDirectories(HOME);
        Path peerCache = HOME.resolve("peerCache.txt");
        return new SavedCompactInfoFileManager(peerCache);
    }
    private final Path peerCache;

    SavedCompactInfoFileManager(Path peerCache) {
        this.peerCache = peerCache;
    }

    public SavedCompactInfo readCompactInfo() {
        try (
                InputStream fis = Files.newInputStream(this.peerCache, StandardOpenOption.READ);
                ObjectInputStream ois = new ObjectInputStream(fis);
        ) {
            return (SavedCompactInfo) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            return SavedCompactInfo.empty();
        }
    }

    public void saveCompactInfo(ByteBuffer myself, List<Node> nodes) {
        try (
                OutputStream fos = Files.newOutputStream(this.peerCache, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
        ) {
            SavedCompactInfo of = SavedCompactInfo.of(myself, nodes);
            oos.writeObject(of);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
