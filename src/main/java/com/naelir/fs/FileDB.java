package com.naelir.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
public class FileDB implements IFileDB {
    public static final Logger logger = LogManager.getLogger(FileDB.class);
    private static final String SEP = "#";
    private static final int SHARD_PREFIX_LEN = 3;
    public static final ObjectMapper MAPPER = new ObjectMapper();
    /** Base directory: ~/filedb/ */
    static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    static final Path BASE_DIR = HOME.resolve("filedb");

    public static void push(String file) {
        Path path = HOME.resolve(file);
        int i = 0;
        try (
                IFileDB db = of();
                BufferedReader reader = Files.newBufferedReader(path)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                Entry value = MAPPER.readValue(line, Entry.class);
                db.insert(value);
            }
            System.out.println(i);
        } catch (Exception e) {
            logger.error("on line {}", i, e);
        }
    }
    
    public static IFileDB of() throws IOException {
        Files.createDirectories(BASE_DIR);
        Files.createDirectories(HOME);
        Path fine = HOME.resolve("fine.txt");
        Path failtoresolve = HOME.resolve("unresolved.txt");
        Path resolved = HOME.resolve("resolved.txt");
        BufferedWriter fw = Files.newBufferedWriter(fine, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        BufferedWriter ftrw = Files.newBufferedWriter(failtoresolve, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        BufferedWriter rw = Files.newBufferedWriter(resolved, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        return new FileDB(fw, ftrw, rw);
    }

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

    private BufferedWriter fineWriter;
    private BufferedWriter unresolvedWriter;
    private BufferedWriter resolvedWriter;

    private FileDB(BufferedWriter dr, BufferedWriter udr, BufferedWriter rw) {
        this.fineWriter = dr;
        this.unresolvedWriter = udr;
        this.resolvedWriter = rw;
    }

    @Override
    public void close() throws Exception {
        fineWriter.close();
        unresolvedWriter.close();
        resolvedWriter.close();
    }
    
    @Override
    public void insert(Entry fr) {
        try {
            String prefix = shardPrefixForId(fr.hash);
            Path shard = shardPath(prefix);
            try (BufferedWriter writer = Files.newBufferedWriter(shard, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE)) {
                String json = MAPPER.writeValueAsString(fr);
                String entryLine = toEntryLine(fr.hash, json);
                writer.write(entryLine);
                writer.newLine();
                
                resolvedWriter.write(entryLine);
                resolvedWriter.newLine();
                resolvedWriter.flush();
            }
            
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }
    
    @Override
    public void fine(Entry fr) {
        try {
            String json = MAPPER.writeValueAsString(fr);            
            fineWriter.write(json);
            fineWriter.newLine();
            fineWriter.flush();
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }
    
    @Override
    public void insertUnresolved(String hash) {
        try {
            unresolvedWriter.write(hash);
            unresolvedWriter.newLine();
            unresolvedWriter.flush();
        } catch (Exception e) {
            logger.error("cannot save", e);
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
    
    @Override
    public List<String> scrape() {
        List<String> result = new ArrayList<>();
        int i = 0;
        try (BufferedReader reader = Files.newBufferedReader(HOME.resolve("scrape.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                if (line.isBlank() || line.length() < 40) {
                    continue;
                }
                result.add(line);                
            }
        } catch (IOException e) {
            logger.error("on line {}", i);
        }

        return result;
    }
    
    @Override
    public List<String> unresolved() {
        List<String> result = new ArrayList<>();
        int i = 0;
        Path path = HOME.resolve("unresolved.txt");
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                result.add(line);
            }
        } catch (IOException e) {
            logger.error("on line {}", i);
        }

        return result;
    }
}