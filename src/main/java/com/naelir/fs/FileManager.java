package com.naelir.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
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

public class FileManager {
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    public static final Logger logger = LogManager.getLogger(FileManager.class);

    public static void main(String[] args) throws Exception {
        FileManager fm = FileManager.of();
        Set<String> unresolved = fm.readUnresolved();
        FileDB fdb = FileDB.of();
        Set<FileRecord> set = new HashSet<>();
        Set<TorrentMeta> meta = fm.readMeta();
        for (TorrentMeta torrentMeta : meta) {
            set.add(new FileRecord(torrentMeta.hash, torrentMeta.getName(), torrentMeta));
        }
        for (String e : unresolved) {
            set.add(new FileRecord(e));
        }
        fdb.create(set);
        fdb.close();
    }

    public static FileManager of() throws IOException {
        Files.createDirectories(HOME);
        Path ts = HOME.resolve("torrents.txt");
        Path done = HOME.resolve("done.".concat(RandomStringUtils.randomAlphabetic(5)));
        Path un = HOME.resolve("unresolved.txt");
        if (Files.exists(ts) == false) {
            Files.createFile(ts);
        }
        if (Files.exists(un) == false) {
            Files.createFile(un);
        }
        return new FileManager(ts, done, un);
    }

    private Path cache;
    private Path done;
    private Path unresolved;

    public FileManager(Path cache, Path done, Path unresolved) {
        this.cache = cache;
        this.unresolved = unresolved;
        this.done = done;
    }

    public void convert(String path) {
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

    public Set<TorrentMeta> readMeta() {
        return readMeta(this.cache);
    }

    private Set<TorrentMeta> readMeta(Path path) {
        Set<TorrentMeta> set = new HashSet<>();
        ObjectMapper om = new ObjectMapper();
        try (
                BufferedReader reader = Files.newBufferedReader(path)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 20) {
                    continue;
                }
                try {
                    String[] split = line.split("#");
                    System.out.println(split[0]);
                    TorrentMeta value = om.readValue(split[2], TorrentMeta.class);
                    value.hash = split[0];
                    set.add(value);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return set;
    }

    public Set<String> readUnresolved() {
        Set<String> set = new HashSet<>();
        try (
                BufferedReader reader = Files.newBufferedReader(this.unresolved)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                set.add(line);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return set;
    }

    void saveHashesTo(Set<String> hashes, Path path) {
        try (
                BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            for (String string : hashes) {
                bufferedWriter.append(string);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void saveMeta(String hash, TorrentMeta meta) {
        if (meta == null)
            return;
        try (
                BufferedWriter tw = Files.newBufferedWriter(this.cache, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                BufferedWriter dw = Files.newBufferedWriter(this.done, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        ) {
            ObjectMapper mapper = new ObjectMapper();
            if (NameFilter.match(meta) && meta.getGenre().equals(Genre.XXX) == false) {
                Entry entry = TorrentMeta.toEntry(hash, meta);
                dw.append(mapper.writeValueAsString(entry));
                dw.append(",");
                dw.newLine();
                tw.append(hash);
                tw.append("#");
                tw.append("FINE");
                tw.append("#");
                tw.append(mapper.writeValueAsString(meta));
                tw.newLine();
            } else {
                tw.append(hash);
                tw.append("#");
                tw.append("CRAP");
                tw.append("#");
                tw.append(mapper.writeValueAsString(meta));
                tw.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void saveUnresolved(Set<String> hashes) {
        try (
                BufferedWriter bufferedWriter = Files.newBufferedWriter(this.unresolved, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            for (String string : hashes) {
                bufferedWriter.append(string);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
