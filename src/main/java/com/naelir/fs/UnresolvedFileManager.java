package com.naelir.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple file-backed CRUD store.
 *
 * <p>
 * The backing file is a CSV with {@code #} as separator and three columns:
 *
 * <pre>
 *   id#name#meta
 * </pre>
 *
 * {@code meta} is serialized as a JSON string value using Jackson. The
 * {@code id} column is the unique key.
 */
public class UnresolvedFileManager {
    public static final Logger logger = LogManager.getLogger(UnresolvedFileManager.class);
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    private Path unresolved;

    UnresolvedFileManager(Path done) {
        this.unresolved = done;
    }

    public static UnresolvedFileManager of() {
        Path done = HOME.resolve("unresolved");
        return new UnresolvedFileManager(done);
    }
    
    
    public List<Pair<String, String>> getAll() {
        List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
        int i = 0;
        try (BufferedReader reader = Files.newBufferedReader(unresolved)) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                if (line.isBlank() || line.length() < 40) {
                    continue;
                }
                int lastIndexOf = line.lastIndexOf(",");
                String name = line.substring(0, lastIndexOf);
                String hash = line.substring(lastIndexOf + 1);
                result.add(new ImmutablePair<>(hash, name));
                
            }
        } catch (IOException e) {
            logger.error("on line {}", i);
        }

        return result;
    }

}