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
//    public static void main(String[] args) throws Exception {
//        FileManager fm = FileManager.of();
//        Set<String> unresolved = fm.readUnresolved();
//        FileDB fdb = FileDB.of();
//        Set<FileRecord> set = new HashSet<>();
//        Set<TorrentMeta> meta = fm.readMeta();
//        for (TorrentMeta torrentMeta : meta) {
//            set.add(new FileRecord(torrentMeta.hash, torrentMeta.getName(), torrentMeta));
//        }
////        for (String e : unresolved) {
////            set.add(new FileRecord(e));
////        }
//        fdb.create(set);
//        fdb.close();
//    }

    public static FileManager of() throws IOException {
        Files.createDirectories(HOME);
        Path done = HOME.resolve("done.".concat(RandomStringUtils.randomAlphabetic(5)));
        return new FileManager(done);
    }

    private Path done;

    public FileManager(Path done) {
        this.done = done;
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
}
