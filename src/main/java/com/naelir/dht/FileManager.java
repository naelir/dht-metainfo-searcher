package com.naelir.dht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class FileManager {
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    public static final Logger logger = LogManager.getLogger(FileManager.class);

    public static FileManager of(String saveOn) throws IOException {
        Files.createDirectories(HOME);
        Path ts = HOME.resolve("torrents.txt");
        Path tout = HOME.resolve(saveOn);
        Path un = HOME.resolve("unresolved.txt");
        if (Files.exists(ts) == false) {
            Files.createFile(ts);
        }
        if (Files.exists(un) == false) {
            Files.createFile(un);
        }
        return new FileManager(ts, un, tout);
    }

    private Path tcache;
    private Path unresolved;
    private Path tout;

    public FileManager(Path tcache, Path unresolved, Path tout) {
        this.tcache = tcache;
        this.unresolved = unresolved;
        this.tout = tout;
    }
//    List<Node> readDhtCache() {
//        try (
//                InputStream fis = Files.newInputStream(this.peerCache, StandardOpenOption.READ);
//        ) {
//            List<Node> nodes = new LinkedList<>();
//            int compactInfoLength = 20 + 4 + 2;
//            byte[] compactInfo = new byte[compactInfoLength];
//            while (fis.read(compactInfo) == compactInfoLength) {
//                nodes.add(Node.of(ByteBuffer.wrap(compactInfo), 4));
//            }
//            return nodes;
//        } catch (IOException e) {
//            return Collections.emptyList();
//        }
//    }

    public Set<String> readHashes() {
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

    public Set<String> readMeta() {
        Set<String> set = new HashSet<>();
        try (
                BufferedReader reader = Files.newBufferedReader(this.tcache)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 20) {
                    continue;
                }
                set.add(line.substring(0, 20));
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return set;
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
//    void saveDhtNodes(ByteBuffer myself, List<Node> nodes) {
//        try (
//                OutputStream fos = Files.newOutputStream(this.peerCache, StandardOpenOption.CREATE,
//                        StandardOpenOption.TRUNCATE_EXISTING);
//        ) {
//            fos.write(myself.array());
//            if (nodes == null || nodes.size() == 0)
//                return;
//            fos.write(nodes.get(0).ip.length);
//            ByteBuffer compact = CompactInfo.compactNodes(nodes);
//            fos.write(compact.array());
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

    public void saveHashes(Set<String> hashes) {
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

    public void saveMeta(Collection<Torrent> torrents) {
        try (
                BufferedWriter writer = Files.newBufferedWriter(this.tout, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            for (Torrent torrent : torrents) {
                TorrentMeta meta = torrent.meta();
                if (meta == null) {
                    continue;
                }
                if (NameFilter.match(meta.getName(), true) && meta.getGenre().equals(Genre.XXX) == false) {
                    writer.append(torrent.infoHash());
                    writer.append("#");
                    writer.append("FINE");
                    writer.append("#");
                    writer.append(mapper.writeValueAsString(meta));
                    writer.newLine();
                } else {
                    writer.append(torrent.infoHash());
                    writer.append("#");
                    writer.append("CRAP");
                    writer.append("#");
                    writer.append(mapper.writeValueAsString(meta));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void saveMeta(String hash, TorrentMeta meta) {
        if (meta == null)
            return;
        try (
                BufferedWriter writer = Files.newBufferedWriter(this.tout, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            if (NameFilter.match(meta) && meta.getGenre().equals(Genre.XXX) == false) {
                writer.append(hash);
                writer.append("#");
                writer.append("FINE");
                writer.append("#");
                writer.append(mapper.writeValueAsString(meta));
                writer.newLine();
            } else {
                writer.append(hash);
                writer.append("#");
                writer.append("CRAP");
                writer.append("#");
                writer.append(mapper.writeValueAsString(meta));
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    

    private static final Pattern PR_FILE_COUNT = Pattern.compile("<span class=torrent_files style=color:#666;padding-left:10px>(\\d+)</span>");
    private static final Pattern AGO = Pattern.compile("found (.+?)<.+");
    private static final Pattern SIZE = Pattern.compile("([\\d\\.]+?)&nbsp;([MBGK]+)");
    private static final Pattern HASH_NAME = Pattern.compile("urn:btih:(.{40}).+?dn=(.+?)&");
    //
    static BtDiggMeta parse(String line) {
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
        
        int multiplier = "KB".equals(suf) ? 1024 : "MB".equals(suf) ? 1024 * 1024 : "GB".equals(suf) ? 1024 * 1024 * 1024 : 0;
        long sizel = (long) (Float.valueOf(size) * multiplier);
        int c = Integer.parseInt(count0);
        c = c == 0 ? 1: c;
        return new BtDiggMeta(c, ago, sizel, hash, name);
    }
    
    public void readBtDigg(Path path) throws IOException {
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
                        BtDiggMeta meta = parse(line);
                        if (meta.filesCount > 100) {
                            System.err.println(meta);
                            continue;
                        }
                        
                        MetaFile me = new MetaFile(meta.name, meta.size);
                        TorrentMeta name = new TorrentMeta(meta.name, List.of(me));
                        name.count = meta.filesCount;
                        Torrent e2 = new Torrent(meta.hash);
                        e2.setMeta(name);
                        list.add(e2);
                    }
                    saveMeta(list);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
//        ObjectMapper name = new ObjectMapper();
//        String writeValueAsString = name.writeValueAsString(list);
//        Files.writeString(Path.of("xaxaxax"), writeValueAsString, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    static class BtDiggMeta {
        int filesCount;
        String ago;
        long size;
        String hash;
        String name;
        public BtDiggMeta(int filesCount, String ago, long size, String hash, String name) {
            super();
            this.filesCount = filesCount;
            this.ago = ago;
            this.size = size;
            this.hash = hash;
            this.name = name;
        }
        @Override
        public String toString() {
            return "Meta [hash=" + hash + ", name=" + name + ", filesCount=" + filesCount + ", ago=" + ago + ", size="
                    + size + "]";
        }
        
        
        
    }
}
