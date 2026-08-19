package com.naelir.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
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
public class FileDB implements AutoCloseable {
    public static final Logger logger = LogManager.getLogger(FileDB.class);
    private static final String SEP = "#";
    private static final int SHARD_PREFIX_LEN = 3;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Base directory: ~/filedb/ */
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    private static final Path BASE_DIR = HOME.resolve("filedb");

    public static FileDB of() throws IOException {
        Files.createDirectories(BASE_DIR);
        Files.createDirectories(HOME);
        Path done = HOME.resolve("done.txt");
        BufferedWriter mainwriter = Files.newBufferedWriter(done, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        return new FileDB(mainwriter);
    }

    private static final String HEX_CHARS = "0123456789abcdef";


    /** Returns the shard file for the given prefix string (first {@value #SHARD_PREFIX_LEN} chars of hash, uppercase). */
    private static Path shardPath(String prefix) {
        return BASE_DIR.resolve(prefix.toUpperCase() + ".txt");
    }

    /**
     * Returns the shard prefix for the given record id (first {@value #SHARD_PREFIX_LEN} characters, uppercased).
     */
    private static String shardPrefixForId(String id) {
        if (id.length() < SHARD_PREFIX_LEN)
            throw new IllegalArgumentException("id must be at least " + SHARD_PREFIX_LEN + " characters long, got: '" + id + "'");
        return id.substring(0, SHARD_PREFIX_LEN).toUpperCase();
    }

    private static String toEntryLine(String hash, String json) throws IOException {
        return hash + SEP + json;
    }
    
    private static Entry fromEntryLine(String line) throws IOException {
        int sep1 = line.indexOf(SEP);
        if (sep1 < 0)
            return null;
        String id = line.substring(0, sep1);
        String other = line.substring(sep1 + 1, line.length());
        return MAPPER.readValue(other, Entry.class);
    }

    private BufferedWriter mainwriter;

    private FileDB(BufferedWriter mainwriter) {
        this.mainwriter = mainwriter;
    }

    @Override
    public void close() throws Exception {
        mainwriter.close();
    }
    
    public void create(Entry fr) {
        try {
            String prefix = shardPrefixForId(fr.hash);
            Path shard = shardPath(prefix);
            try (BufferedWriter writer = Files.newBufferedWriter(shard, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE)) {
                String json = MAPPER.writeValueAsString(fr);
                String entryLine = toEntryLine(fr.hash, json);
                writer.write(entryLine);
                writer.newLine();
            }
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }
    
    public void createFine(Entry fr) {
        try {
            String json = MAPPER.writeValueAsString(fr);            
            mainwriter.write(json);
            mainwriter.newLine();
            mainwriter.flush();
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }

    public void importHashNameCsv(String path) throws IOException {
        Path from = HOME.resolve(path);
        try (
                BufferedReader reader = Files.newBufferedReader(from);
        ) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                i++;
                String[] split = line.split("#");
                var entry = new Entry(split[1], split[0], 1, 0, 0, "NA", 0);
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                create(entry);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    public void importDone(String path) throws IOException {
        Path from = HOME.resolve(path);
        int i = 0;

        try (
                BufferedReader reader = Files.newBufferedReader(from);
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                if (line.startsWith("{") == false) {
                    continue;
                }
                Entry e = MAPPER.readValue(line, Entry.class);
                create(e);
            }
            System.out.println(i);
        } catch (IOException e) {
            logger.error("line {}", i, e);
        }
    }

    public String get(String hash) {
        String prefix = shardPrefixForId(hash);
        Path shard = shardPath(prefix);
        if (!Files.exists(shard))
            return null;
        try (BufferedReader reader = Files.newBufferedReader(shard)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int sep = line.indexOf(SEP);
                if (sep >= 0 && line.substring(0, sep).equalsIgnoreCase(hash)) {
                    return line.substring(sep + 1, line.length());
                }
            }
        } catch (IOException e) {
            logger.error("cannot check contains for hash {}", hash, e);
        }
        return null;
    }
}