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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
import com.naelir.bt.NameFilter;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.bt.TorrentMeta.Genre;
import com.naelir.bt.TorrentMeta.MetaFile;

public class ThirdPartyFileManager {
    private static class Meta {
        int filesCount;
        String ago;
        long size;
        String hash;
        String name;

        public Meta(int filesCount, String ago, long size, String hash, String name) {
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

    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    public static final Logger logger = LogManager.getLogger(ThirdPartyFileManager.class);

    private static final Pattern PR_FILE_COUNT = Pattern
            .compile("<span class=torrent_files style=color:#666;padding-left:10px>(\\d+)</span>");
    private static final Pattern AGO = Pattern.compile("found (.+?)<.+");
    private static final Pattern SIZE = Pattern.compile("([\\d\\.]+?)&nbsp;([MBGK]+)");
    private static final Pattern HASH_NAME = Pattern.compile("urn:btih:(.{40}).+?dn=(.+?)&");
    private static final Pattern LIME = Pattern.compile(
            "<tr.+?><a href=\\\"http:\\/\\/itorrents.net\\/torrent\\/(.+?)\\.torrent\\?title=(.+?)\\\".+?<td class=\\\"tdnormal\\\">(.+?)<.+?<td class=\\\"tdnormal\\\">(.+?) ([KBMGbytes]+)<\\/td><td class=\"tdseed\">(.+?)<\\/td><td class=\"tdleech\">(\\d+)<\\/td>");

    public void convertPages(Path path) throws IOException {
        String random = RandomStringUtils.randomAlphabetic(10);
        Path to = HOME.resolve(random);
        Path toC = HOME.resolve(random);
        try (
                BufferedWriter writer = Files.newBufferedWriter(to, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                BufferedWriter writerC = Files.newBufferedWriter(toC, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
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
                                int indexOf = line.indexOf("Previous");
                                if (indexOf < 0) {
                                    continue;
                                }
                                line = line.substring(indexOf, line.length());
                            }
                            Meta meta = parse(line);
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
                            if (NameFilter.match(name) && name.getGenre().equals(Genre.XXX) == false) {
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
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void convertLimePages(String path) {
        String random = RandomStringUtils.randomAlphabetic(10);
        Path to = HOME.resolve(random);
        Path toC = HOME.resolve(random);
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
                    if (NameFilter.match(meta) && meta.getGenre().equals(Genre.XXX) == false) {
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
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    Meta parse(String line) {
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
        return new Meta(c, ago, sizel, hash, name);
    }
}
