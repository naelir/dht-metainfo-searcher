package com.naelir.dht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.NameFilter;
import com.naelir.bt.Torrent;
import com.naelir.bt.TorrentMeta;
import com.naelir.bt.TorrentMeta.Genre;

public class FileManager {
    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    public static final Logger logger = LogManager.getLogger(FileManager.class);

    public static FileManager of() throws IOException {
        Files.createDirectories(HOME);
        Path ts = HOME.resolve("torrents.txt");
        Path un = HOME.resolve("unresolved.txt");
        if (Files.exists(ts) == false) {
            Files.createFile(ts);
        }
        if (Files.exists(un) == false) {
            Files.createFile(un);
        }
        return new FileManager(ts, un);
    }

//    private Path peerCache;
    private Path tcache;
    private Path unresolved;

    public FileManager(Path tcache, Path unresolved) {
//        this.peerCache = peerCache;
        this.tcache = tcache;
        this.unresolved = unresolved;
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
                BufferedWriter writer = Files.newBufferedWriter(this.tcache, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper mapper = new ObjectMapper();
            for (Torrent torrent : torrents) {
                TorrentMeta meta = torrent.meta();
                if (meta == null) {
                    continue;
                }
                if (NameFilter.match(meta) && meta.getGenre().equals(Genre.XXX) == false) {
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
                BufferedWriter writer = Files.newBufferedWriter(this.tcache, StandardOpenOption.CREATE,
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
}
