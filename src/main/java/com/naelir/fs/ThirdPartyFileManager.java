package com.naelir.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
import com.naelir.bt.NameFilter;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.bt.TorrentMeta.Genre;
import com.naelir.bt.TorrentMeta.MetaFile;
import com.naelir.http.HttpsClient;
import com.naelir.http.HttpsClient.HttpsClientException;

public class ThirdPartyFileManager {
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    public static final Logger logger = LogManager.getLogger(ThirdPartyFileManager.class);
    private static final Pattern PR_FILE_COUNT = Pattern
            .compile("<span class=torrent_files style=color:#666;padding-inline-start:10px>(\\d+)</span>");
    private static final Pattern AGO = Pattern.compile("found (.+?)<.+");
    private static final Pattern SIZE = Pattern.compile("([\\d\\.]+?)&nbsp;([MBGK]+)");
    private static final Pattern HASH_NAME = Pattern.compile("urn:btih:(.{40}).+?dn=(.+?)&");
    private static final Pattern UI = Pattern.compile("magnet:\\?xt=urn:btih:(.{40,40}).+torrent-link.*>(.+?)<\\/a><td class=sr-col-size>(.+?) ([GBKkM]+)");

    private static final Pattern LIME = Pattern.compile(
            "magnet:\\?xt=urn:btih:(.{40,40}).+torrent-link.+?>(.+?)<\\/a><td class=sr-col-size>(.+?) ([GBKkM]+)");

    public void convertUiPages(String path) {
        Path to = HOME.resolve(RandomStringUtils.randomAlphabetic(10));
        Path from = HOME.resolve(path);
        try (
                BufferedReader reader = Files.newBufferedReader(from);
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        ) {
            ObjectMapper mapper = new ObjectMapper();
            String line;
            int i = 0;
            writer.append("[");
            while ((line = reader.readLine()) != null) {
                i++;
                if (i % 1000 == 0) {
                    logger.info("processed {} lines", i);
                }
                String[] split = line.split("<tr>");
                for (String s : split) {
                    extracted(writer, mapper, s);
                }
                
            }
            writer.append("]");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    void extracted(BufferedWriter writer, ObjectMapper mapper, String line)
            throws IOException, JsonProcessingException {
        Matcher matcher00 = UI.matcher(line);
        while (matcher00.find()) {
            String hash = matcher00.group(1).toUpperCase();
            String name = matcher00.group(2).replace(" ", ".").concat("[ui]");
            String sizeS = matcher00.group(3);
            String suf = matcher00.group(4);
            int multiplier = "KB".equals(suf) ? 1024
                    : "MB".equals(suf) ? 1024 * 1024 : "GB".equals(suf) ? 1024 * 1024 * 1024 : 0;
            long size = 0;
            try {
                size = (long) (Float.valueOf(sizeS) * multiplier);

            } catch (NumberFormatException e) {
                // TODO: handle exception
            }
            TorrentMeta meta = new TorrentMeta(hash, name, List.of(new MetaFile(name, Long.valueOf(size))));
            Entry entry = TorrentMeta.toEntry(hash, meta);
                writer.append(mapper.writeValueAsString(entry));
            writer.append(",");
            writer.newLine();
            writer.flush();
        }
    }
    
    public void convertLimePages(String path) {
        Path to = HOME.resolve(RandomStringUtils.randomAlphabetic(10));
        Path toC = HOME.resolve(RandomStringUtils.randomAlphabetic(10));
        Path from = HOME.resolve(path);
        try (
                BufferedReader reader = Files.newBufferedReader(from);
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                BufferedWriter writerC = Files.newBufferedWriter(toC, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            String line;
            int i = 0;
            writer.append("[");
            writerC.append("[");
            while ((line = reader.readLine()) != null) {
                i++;
                if (i % 1000 == 0) {
                    logger.info("processed {} lines", i);
                }
                Matcher matcher00 = LIME.matcher(line);
                while (matcher00.find()) {
                    String hash = matcher00.group(1);
                    String name = matcher00.group(2);
                    String ago = matcher00.group(3);
                    String sizeS = matcher00.group(4);
                    String suf = matcher00.group(5);
                    String seed = matcher00.group(6);
                    int multiplier = "KB".equals(suf) ? 1024
                            : "MB".equals(suf) ? 1024 * 1024 : "GB".equals(suf) ? 1024 * 1024 * 1024 : 0;
                    long size = (long) (Float.valueOf(sizeS) * multiplier);
                    TorrentMeta meta = new TorrentMeta(hash, name, List.of(new MetaFile(name, Long.valueOf(size))));
                    Entry entry = TorrentMeta.toEntry(hash, meta);
                    if (NameFilter.match(name) && meta.getGenre().equals(Genre.XXX) == false) {
                        writer.append(mapper.writeValueAsString(entry));
                        writer.append(",");
                        writer.newLine();
                        writer.flush();
                    } else {
                        writerC.append(mapper.writeValueAsString(entry));
                        writerC.append(",");
                        writerC.newLine();
                        writerC.flush();
                    }
                }
            }
            writer.append("]");
            writerC.append("]");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void convertBtPages(Path path) throws IOException {
        String random = RandomStringUtils.randomAlphabetic(10);
        Path to = HOME.resolve(random);
        try (
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        ) {
            ObjectMapper mapper = new ObjectMapper();
            Files.walk(path).forEach(e -> {
                try {
                    if (Files.isDirectory(e) == false) {
                        System.out.println(e.getFileName());
                        String lines = Files.readString(e);
                        String[] split = lines.split("\\.\\.\\.");
                        List<Torrent> list = new ArrayList<Torrent>();
                        for (int i = 0; i < split.length; i++) {
                            String line = split[i];
                            if (i == 0) {
                                int indexOf = line.indexOf("다음 →");
                                if (indexOf < 0) {
                                    continue;
                                }
                                line = line.substring(indexOf, line.length());
                            }
                            BtMeta meta = parse(line);
                            if (meta.filesCount > 100) {
                                System.err.println(meta);
                                continue;
                            }
                            MetaFile me = new MetaFile(meta.name, meta.size);
                            TorrentMeta name = new TorrentMeta(meta.hash, meta.name, List.of(me));
                            name.count = meta.filesCount;
                            Torrent e2 = new Torrent(meta.hash);
                            e2.setMeta(name);
                            list.add(e2);
                            Entry entry = TorrentMeta.toEntry(meta.hash, name);
                            if (NameFilter.match(meta.name)) {
                                writer.append(mapper.writeValueAsString(entry));
                                writer.append(",");
                                writer.newLine();
                                writer.flush();
                            }
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    public void convertRarbg(Path path) {
        String random = RandomStringUtils.randomAlphabetic(10);
        Path to = HOME.resolve(random);
        try (
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        ) {
            ObjectMapper mapper = new ObjectMapper();

            Pattern date = Pattern.compile("\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d");
            Files.walk(path).forEach(e -> {

                if (Files.isDirectory(e) == false) {
                try (
                        BufferedReader reader = Files.newBufferedReader(e);
                ) {
                        String line;
                        int i = 0;
                        while ((line = reader.readLine()) != null) {
                            i++;
                            if (i % 1000 == 0) {
                                logger.info("processed {} lines", i);
                            }
                            String hash = line.substring(20, 60);
                            String name = line.substring(64, line.length());
                            if (NameFilter.TV_SERIES.matcher(name).find() || date.matcher(name).find()) {
                                continue;
                            }
                            TorrentMeta meta = new TorrentMeta(hash, name, List.of(new MetaFile(name, 0L)));
                            Entry entry = TorrentMeta.toEntry(hash, meta);
                            entry.genre = "MUSIC";
//                            if (NameFilter.match(name, true) && meta.getGenre().equals(Genre.XXX) == false) {
                                writer.append(mapper.writeValueAsString(entry));
                                writer.append(",");
                                writer.newLine();
                                writer.flush();
//                            }
                        }
                    
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }});
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    BtMeta parse(String line) {
        Matcher matcher00 = PR_FILE_COUNT.matcher(line);
        Matcher matcher02 = AGO.matcher(line);
        Matcher matcher03 = SIZE.matcher(line);
        Matcher matcher04 = HASH_NAME.matcher(line);
        String count0 = matcher00.find() ? matcher00.group(1) : "0";
        String ago = matcher02.find() ? matcher02.group(1) : "";
        boolean b = matcher03.find();
        String size = b ? matcher03.group(1) : "0";
        String suf = b ? matcher03.group(2) : "";
        boolean b1 = matcher04.find();
        String hash = b1 ? matcher04.group(1) : "";
        String name = b1 ? matcher04.group(2) : "";
        int multiplier = "KB".equals(suf) ? 1024
                : "MB".equals(suf) ? 1024 * 1024 : "GB".equals(suf) ? 1024 * 1024 * 1024 : 0;
        long sizel = (long) (Float.valueOf(size) * multiplier);
        int c = Integer.parseInt(count0);
        c = c == 0 ? 1 : c;
        name = name.replaceAll("%5B", "[");
        name = name.replaceAll("%5D", "]");
        return new BtMeta(c, ago, sizel, hash, name);
    }

    
    public String limePages(int lastMoviePage, int lastTvPage, int lastGamePage) throws HttpsClientException, InterruptedException {
        Map<String, Integer> list = Map.of(
                "https://www.limetorrents.fun/browse-torrents/Movies/date/", lastMoviePage,
                "https://www.limetorrents.fun/browse-torrents/TV-shows/date/", lastTvPage,
                "https://www.limetorrents.fun/browse-torrents/Games/date/", lastGamePage
                );
        String n = RandomStringUtils.randomAlphanumeric(10);
        Path path = Paths.get(System.getProperty("user.home")).resolve(n);
        try (
                HttpsClient name = new HttpsClient();
                BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            for (java.util.Map.Entry<String, Integer> e : list.entrySet()) {
                for (int i = 1; i < e.getValue(); i++) {
                    String body = name.get(e.getKey().concat(Integer.toString(i)));
                    bufferedWriter.append(body);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    Thread.sleep(1000);
                    System.out.println(i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n;
    }

    private static class BtMeta {
        int filesCount;
        String ago;
        long size;
        String hash;
        String name;

        public BtMeta(int filesCount, String ago, long size, String hash, String name) {
            this.filesCount = filesCount;
            this.ago = ago;
            this.size = size;
            this.hash = hash;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Meta [hash=" + this.hash + ", name=" + this.name + ", filesCount=" + this.filesCount + ", ago="
                    + this.ago + ", size=" + this.size + "]";
        }
    }
}
