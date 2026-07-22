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
    private static final Path BASE_DIR = Path.of(System.getProperty("user.home")).resolve("filedb");
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");

    /** Escapes newlines and the separator character inside field values. */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace(SEP, "\\" + SEP);
    }

    private static FileRecord fromLine(String line) throws IOException {
        int sep1 = line.indexOf(SEP);
        if (sep1 < 0)
            return null;
        int sep2 = line.indexOf(SEP, sep1 + 1);
        if (sep2 < 0)
            return null;
        String id = unescape(line.substring(0, sep1));
        String name = unescape(line.substring(sep1 + 1, sep2));
//        TorrentMeta meta = MAPPER.readValue(line.substring(sep2 + 1), TorrentMeta.class);
        return new FileRecord(id, name, null);
    }

    public static void main(String[] args) throws IOException {
        FileDB of = FileDB.of();
        Optional<FileRecord> optional = of.get("0C7251C5DDC4324547A0397D56123BCB2E4EF549");
        System.out.println("here");
    }

    public static FileDB of() throws IOException {
        Files.createDirectories(BASE_DIR);
        Map<Path, BufferedWriter> writers = new HashMap<>();
        for (char c : HEX_CHARS.toCharArray()) {
            Path shard = shardPath(c);
            if (!Files.exists(shard)) {
                Files.createFile(shard);
            }
            writers.put(shard, Files.newBufferedWriter(shard, java.nio.file.StandardOpenOption.APPEND));
        }
        Files.createDirectories(HOME);
        Path done = HOME.resolve("done.".concat(RandomStringUtils.randomAlphabetic(5)));
        return new FileDB(writers, done);
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
    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    private static String toLine(FileRecord record) throws IOException {
        String metaJson = record.getMeta() != null ? MAPPER.writeValueAsString(record.getMeta()) : "";
        return escape(record.getId()) + SEP + escape(record.getName()) + SEP + metaJson;
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
    // UPDATE
    // -------------------------------------------------------------------------

    private static void writeShard(Path shard, List<FileRecord> records) throws IOException {
        Path tmp = shard.resolveSibling(shard.getFileName() + ".tmp");
        try (
                BufferedWriter writer = Files.newBufferedWriter(tmp)
        ) {
            for (FileRecord r : records) {
                writer.write(toLine(r));
                writer.newLine();
            }
        }
        Files.move(tmp, shard, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    private Map<Path, BufferedWriter> writers;
    private Path done;

    private FileDB(Map<Path, BufferedWriter> writers, Path done) {
        this.writers = writers;//
        this.done = done;
    }
    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    @Override
    public void close() throws Exception {
        for (BufferedWriter bw : this.writers.values()) {
            bw.close();
        }
    }

    /**
     * Appends a new record.
     *
     * @throws IllegalArgumentException if a record with the same id already exists
     */
    public void create(FileRecord record) {
        try {
            Optional<FileRecord> existing = get(record.getId());
            if (existing.isPresent()) {
                if (FileRecord.DEFAULT_NAME.equals(existing.get().getName())) {
                    update(record);
                }
                return;
            }
            Path shard = shardPathForId(record.getId());
            BufferedWriter writer = this.writers.get(shard);
            writer.write(toLine(record));
            writer.newLine();
        } catch (Exception e) {
            logger.error("cannot save", e);
        }
    }

    public void create(Set<FileRecord> records) throws IOException {
        for (FileRecord fr : records) {
            Path shard = shardPathForId(fr.getId());
            BufferedWriter writer = this.writers.get(shard);
            writer.write(toLine(fr));
            writer.newLine();
            System.out.println(fr.hashCode());
        }
    }

    /**
     * Deletes the record with the given id.
     *
     * @return {@code true} if the record was found and deleted, {@code false}
     *         otherwise
     */
    public boolean delete(String id) throws IOException {
        Path shard = shardPathForId(id);
        List<FileRecord> shardRecords = readShard(shard);
        boolean removed = shardRecords.removeIf(r -> r.getId().equals(id));
        if (removed) {
            writeShard(shard, shardRecords);
        }
        return removed;
    }

    /**
     * Returns the record with the given id, or {@link Optional#empty()} if not
     * found.
     */
    public Optional<FileRecord> get(String id) throws IOException {
        Path shard = shardPathForId(id);
        int i = 0;
        try (
                BufferedReader reader = Files.newBufferedReader(shard)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                i++;
                if (line.isBlank()) {
                    continue;
                }
//                System.out.println(i);
                FileRecord record = fromLine(line);
                if (record != null && record.getId().equals(id))
                    return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all records in the file.
     */
    public List<FileRecord> getAll() throws IOException {
        List<FileRecord> result = new ArrayList<>();
        for (char c : HEX_CHARS.toCharArray()) {
            try (
                    BufferedReader reader = Files.newBufferedReader(shardPath(c))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    FileRecord record = fromLine(line);
                    if (record != null) {
                        result.add(record);
                    }
                }
            }
        }
        return result;
    }

    public void metaToEntry(String path) {
        Set<Entry> list = new HashSet<>();
        String random = RandomStringUtils.randomAlphabetic(10);
        Path to = HOME.resolve(random);
        Path from = HOME.resolve(path);
        try (
                BufferedReader reader = Files.newBufferedReader(from);
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("#");
                if ("FINE".equals(split[1])) {
                    Torrent torrent = new Torrent(split[0]);
                    TorrentMeta value = mapper.readValue(split[2], TorrentMeta.class);
                    list.add(TorrentMeta.toEntry(torrent.infoHash(), value));
                }
            }
            writer.write(mapper.writeValueAsString(list));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private List<FileRecord> readShard(Path shard) throws IOException {
        List<FileRecord> result = new ArrayList<>();
        try (
                BufferedReader reader = Files.newBufferedReader(shard)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                FileRecord record = fromLine(line);
                if (record != null) {
                    result.add(record);
                }
            }
        }
        return result;
    }

    public void saveMeta(String hash, TorrentMeta meta) {
        if (meta == null)
            return;
        try (
                BufferedWriter dw = Files.newBufferedWriter(this.done, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        ) {
            ObjectMapper mapper = new ObjectMapper();
            if (NameFilter.match(meta) && meta.getGenre().equals(Genre.XXX) == false) {
                Entry entry = TorrentMeta.toEntry(hash, meta);
                dw.append(mapper.writeValueAsString(entry));
                dw.append(",");
                dw.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Replaces the name of the record with the given id.
     *
     * @return {@code true} if the record was found and updated, {@code false}
     *         otherwise
     */
    public boolean update(FileRecord updated) throws IOException {
        Path shard = shardPathForId(updated.getId());
        List<FileRecord> shardRecords = readShard(shard);
        boolean found = false;
        for (int i = 0; i < shardRecords.size(); i++) {
            if (shardRecords.get(i).getId().equals(updated.getId())) {
                shardRecords.set(i, updated);
                found = true;
                break;
            }
        }
        if (found) {
            writeShard(shard, shardRecords);
        }
        return found;
    }
}