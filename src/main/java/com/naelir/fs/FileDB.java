package com.naelir.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
import com.naelir.bt.NameFilter;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.bt.TorrentMeta.Genre;

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
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Base directory: ~/filedb/ */
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    private static final Path BASE_DIR = HOME.resolve("filedb");

    /** Escapes newlines and the separator character inside field values. */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace(SEP, "\\" + SEP);
    }

    public static FileDB of() throws IOException {
        Files.createDirectories(BASE_DIR);
        Path done = HOME.resolve("done.".concat(RandomStringUtils.randomAlphabetic(5)));

        BufferedWriter mainwriter = Files.newBufferedWriter(done, java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        Map<Path, BufferedWriter> writers = new HashMap<>();
        for (char c : HEX_CHARS.toCharArray()) {
            Path shard = shardPath(c);
            if (!Files.exists(shard)) {
                Files.createFile(shard);
            }
            writers.put(shard, Files.newBufferedWriter(shard, java.nio.file.StandardOpenOption.APPEND));
        }
        Files.createDirectories(HOME);
        return new FileDB(writers, mainwriter);
    }

    /** Returns the shard file for the given hex character (0-f). */
    private static Path shardPath(char hexChar) {
        return BASE_DIR.resolve(hexChar + ".txt");
    }
    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Returns the shard file for the given record id (based on its first hex
     * character).
     */
    private static Path shardPathForId(String id) {
        char first = Character.toLowerCase(id.charAt(0));
        if (HEX_CHARS.indexOf(first) < 0)
            throw new IllegalArgumentException("id must start with a hex character (0-9, a-f), got: '" + id + "'");
        return shardPath(first);
    }

    private static String toEntryLine(String hash, String json) throws IOException {
        return escape(hash) + SEP + json;
    }
    
    private static Entry fromEntryLine(String line) throws IOException {
        int sep1 = line.indexOf(SEP);
        if (sep1 < 0)
            return null;
        String id = unescape(line.substring(0, sep1));
        String other = unescape(line.substring(sep1 + 1, line.length()));
        return MAPPER.readValue(other, Entry.class);
    }
    
    private static String unescape(String value) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                switch (c) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case '\\' -> sb.append('\\');
                default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    private Map<Path, BufferedWriter> writers;
    private BufferedWriter mainwriter;

    private FileDB(Map<Path, BufferedWriter> writers, BufferedWriter mainwriter) {
        this.writers = writers;//
        this.mainwriter = mainwriter;
    }
    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    @Override
    public void close() throws Exception {
        for (BufferedWriter bw : this.writers.values()) {
            bw.close();
        }
        mainwriter.close();
    }
    
    public void create(Entry fr) {
        try {
            Path shard = shardPathForId(fr.hash);
            BufferedWriter writer = this.writers.get(shard);
            String json = MAPPER.writeValueAsString(fr);

            String entryLine = toEntryLine(fr.hash, json);
            writer.write(entryLine);
            writer.newLine();
            
            mainwriter.write(json);
            mainwriter.newLine();
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }
    
    public List<Entry> getAll(String hash) {
        List<Entry> result = new ArrayList<>();
        int i = 0;
        try {
            List<BufferedReader> list = readers(hash);
            for (BufferedReader reader : list) {
                String line;
                while ((line = reader.readLine()) != null) {
                    i++;
                    if (line.isBlank()) {
                        continue;
                    }
                    Entry record = fromEntryLine(line);
                    if (record != null) {
                        result.add(record);
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            logger.error("on line {}", i);
        }
        
        return result;
    }

    private List<BufferedReader> readers(String c) throws IOException {
        char first = c.charAt(0);
        char second = c.charAt(1);
        if (second > '8') {
            if (first == 'f') {
                return List.of(Files.newBufferedReader(shardPath(first)));
            } else {
                return List.of(Files.newBufferedReader(shardPath(first)), Files.newBufferedReader(shardPath(first++)));
            }
        } else {
            if (first == '0') {
                return List.of(Files.newBufferedReader(shardPath(first)));
            } else {
                return List.of(Files.newBufferedReader(shardPath(first)), Files.newBufferedReader(shardPath(first--)));
            }
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
                var entry = new Entry(split[1], split[0], 1, 0, 0, "NA");
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
                Entry e = MAPPER.readValue(line, Entry.class);
                create(e);
            }
        } catch (IOException e) {
            logger.error("line {}", i, e);
        }
    }
    
    public static void main(String[] args) throws IOException {
        FileDB of = FileDB.of();
        of.importDone("all");
    }

}