package com.naelir.dht;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.naelir.bt.Entry;
import com.naelir.bt.NameFilter;
import com.naelir.bt.Torrent;
import com.naelir.fs.FileDB;
import com.naelir.fs.IpBlocker;
import com.naelir.tasks.Sample;

public class DhtResponseResolver {
    public static final Logger logger = LogManager.getLogger(DhtResponseResolver.class);
    private Data data;
    private Cache<String, Boolean> ipcache;

    public DhtResponseResolver(Data data) {
        this.data = data;
        this.ipcache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(1)).build();
    }

    private Object forAddress(From from) {
        if (from.ip == null || from.ip.length != 4)
            return "0.0.0.0";
        return (from.ip[0] & 0xFF) + "." + (from.ip[1] & 0xFF) + "." + (from.ip[2] & 0xFF) + "." + (from.ip[3] & 0xFF);
    }

    private void logFrom(Object decode, From from) {
        logger.debug("{}, from {}, port {}", decode, forAddress(from), from.port);
    }

    private void logTo(Object decode, From from) {
        logger.debug("{}, to {}, port {}", decode, forAddress(from), from.port);
    }

    private Optional<byte[]> optional(byte[] encode) {
        if (encode != null && encode.length > 0)
            return Optional.of(encode);
        else
            return Optional.empty();
    }

    private Object resolve(AnnouncePeerRequest message, From from) {
        Node found = this.data.tokensSent.get(message.token);
        ByteBuffer hashed = Token.token(from.ip);
        if (found != null && message.token.equals(hashed)) {
            Integer remotePort = message.port;
            if (message.implied != null && message.implied > 0) {
                remotePort = from.port;
            }
            Node node = new Node(from.ip, remotePort, message.id);
            String hex = Generator.toHex(message.infoHash.array());
            Torrent previous = this.data.torrents.get(hex);
            if (previous != null) {
                previous.peers().add(node);
            } else {
                Torrent name = new Torrent(hex).addPeer(node);
                this.data.torrents.put(hex, name);
            }
            return new AnnouncePeerResponse(message.tid, this.data.myself, message);
        } else
            return new Error(203, "invalid token", message.tid);
    }

    private void resolve(AnnouncePeerResponse decode, From from) {
        if (decode.request instanceof AnnouncePeerRequest apr) {
            apr.node.put(Command.ANNOUNCE_R);
        }
    }

    public Optional<byte[]> resolve(BencodedDictionary map, From from) {
        try {
            String type = KRPCKeys.getType(map);
            if (KRPCKeys.QUERY.equals(type)) {
                Object decode = CommandDecoder.decodeRequest(map);
                logFrom(decode, from);
                return resolve(decode, from);
            } else if (KRPCKeys.RESPONSE.equals(type)) {
                ByteBuffer tid = KRPCKeys.getTransaction(map);
                IRequest found = this.data.requestsSent.remove(tid);
                if (found != null) {
                    Object decode = CommandDecoder.decodeResponse(map, found);
                    logFrom(decode, from);
                    return resolve(decode, from);
                }
            } else {
                ByteBuffer tid = KRPCKeys.getTransaction(map);
                if (tid != null) {
                    IRequest found = this.data.requestsSent.remove(tid);
                    if (found != null) {
                        Object decode = CommandDecoder.decodeError(map, found);
                        logFrom(decode, from);
                        return resolve(decode, from);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return Optional.empty();
    }

    private IResponse resolve(FindNodeRequest message, From from) {
        String ip = Generator.ip(from.ip);
        if (ipcache.getIfPresent(ip) != null) {
            logger.debug("find node from {} will return error, scanners spam", from);
            return new Error(201, "too many requests", message.tid);
        } else {
            ipcache.put(ip, Boolean.TRUE);
            List<Node> nodes = this.data.table.closest(message.target);
            logger.debug("find node from {} {} resolved, returning {} close nodes", Generator.toHex(message.target.array()),
                   from, nodes.size());
            return new FindNodeResponse(message.tid, this.data.myself, nodes, message);
        }
    }

    private Optional<byte[]> resolve(FindNodeResponse decode, From from) {
        decode.request.node.put(Command.FIND_NODE_R);
        if (decode.request.target == data.myself) {
            for (Node node : decode.nodes) {
                Pair<String, String> location = data.locationDb.location(node.ip);
                if (IpBlocker.denied(location) == false || this.data.table.size() < 5) {
                    this.data.table.insert(node);
                    node.setLocation(location);
                } else {
                    logger.debug("node {} from {} denied", node, location);
                }
            }
        } else {
            String hex = Generator.toHex(decode.request.target.array());
            Sample sample = data.samples.get(hex);
            logger.debug("receiving {} nodes for hash {}", decode.nodes.size(), hex);

            decode.nodes.forEach(e -> sample.table().insert(e));
        }
        return Optional.empty();
    }

    private Object resolve(GetPeersRequest message, From from) {
        Token token = new Token(from.ip);
        ByteBuffer infoHash = message.infoHash;
        String hex = Generator.toHex(infoHash.array());
        Torrent torrent = this.data.torrents.get(hex);
        this.data.tokensSent.put(token.value, new Node(from.ip, from.port, message.id));
        List<Node> nodes = this.data.table.closest(infoHash);
        if (torrent != null) {
            List<Node> peers = new ArrayList<>(torrent.peers());
            return new GetPeersResponse(message.tid, this.data.myself, token.value, nodes, peers, message);
        } else
            return new GetPeersResponse(message.tid, this.data.myself, token.value, nodes, Collections.emptyList(),
                    message);
    }

    private Optional<byte[]> resolve(GetPeersResponse decode, From from) {
        IRequest req = decode.request;
        if (req instanceof GetPeersRequest gpr) {
            if (decode.token != null) {
                this.data.tokensReceived.put(decode.token, gpr.node);
            }
            gpr.node.put(Command.GET_PEER_R);
            String hex = Generator.toHex(gpr.infoHash.array());
            Sample sample = this.data.samples.get(hex);
            if (sample != null) {
                int denied = 0;
                for (Node node : decode.peers) {
                    Pair<String, String> location = data.locationDb.location(node.ip);
                    if (IpBlocker.allowed(location)) {
                        sample.addPeer(node);
                        node.setLocation(location);
                    } else {
                        denied++;
                    }
                }
                int size = decode.peers.size();
                if (size > 0 && denied * 100 / size >= 75) {
                    sample.skip(true);
                    logger.debug("{} too many denied peers", hex);
                    if (size == 1) {
                        data.fileManager.create(Entry.lowPeersNotEu(hex));
                    } else {
                        data.fileManager.create(Entry.crap(hex));
                    }
                }
                logger.info("found {} peers for {}, denied {}", size, hex, denied);
                for (Node node : decode.nodes) {
                    Pair<String, String> location = data.locationDb.location(node.ip);
                    if (IpBlocker.denied(location) == false) {
                        sample.table().insert(node);
                        node.setLocation(location);
                    } else {
                        logger.debug("node {} from {} denied", node, location);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> resolve(IRequest decode, From from) {
        if (decode instanceof AnnouncePeerRequest apr) {
            Object r = resolve(apr, from);
            logTo(r, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof GetPeersRequest gpr) {
            Object r = resolve(gpr, from);
            logTo(r, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof FindNodeRequest fnr) {
            IResponse r = resolve(fnr, from);
            logTo(r, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof PingRequest pr) {
            PingResponse r = resolve(pr, from);
            logTo(r, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof SampleInfoHashesRequest sihr) {
            IResponse r = resolve(sihr, from);
            logTo(r, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else
            return Optional.empty();
    }

    private Optional<byte[]> resolve(IResponse decode, From from) {
        if (decode instanceof AnnouncePeerResponse apr) {
            resolve(apr, from);
        } else if (decode instanceof GetPeersResponse gpr) {
            resolve(gpr, from);
        } else if (decode instanceof FindNodeResponse pr) {
            resolve(pr, from);
        } else if (decode instanceof SampleInfoHashesResponse sihr) {
            resolve(sihr, from);
        } else if (decode instanceof PingResponse sihr) {
            resolve(sihr, from);
        }
        return Optional.empty();
    }

    private Optional<byte[]> resolve(Object decode, From from) {
        if (decode instanceof IRequest ir)
            return resolve(ir, from);
        else if (decode instanceof IResponse rsp)
            return resolve(rsp, from);
        else
            return Optional.empty();
    }

    private PingResponse resolve(PingRequest decode, From from) {
        return new PingResponse(decode.tid, this.data.myself, decode);
    }

    private void resolve(PingResponse decode, From from) {
        logger.info("ping response from {}", from);
        decode.request.node.put(Command.PING_R);
    }

    private IResponse resolve(SampleInfoHashesRequest decode, From from) {
        List<Node> nodes = this.data.table.closest(decode.target, 8);
        int values = this.data.samples.size();
        int min = Math.min(20, values);
        List<String> list = new ArrayList<>(this.data.samples.values()).subList(0, min)
                .stream()
                .map(e -> e.torrent().infoHash())
                .toList();
        return new SampleInfoHashesResponse(decode.tid, this.data.myself, 3600, nodes, min, list, decode);
    }

    private void resolve(SampleInfoHashesResponse decode, From from) {//
        if (decode.samples.isEmpty() == false) {
            int i = 0;
            int tooFar = 0;
            for (String hash : decode.samples) {
                String value = this.data.fileManager.get(hash);
                if (value != null) {
                    if (isFine(value)) {
                        data.forUpdate.add(new ImmutablePair<>(hash, 1));
                    }
                    logger.debug("hash {} already resolved as {}", hash, value);
                    i++;
                } else if (closeEnough(decode.request.node, hash)) {
                    this.data.samples.computeIfAbsent(hash, k -> new Sample(new Torrent(k), decode.request.node, false));
                } else {
                    tooFar++;
                }
            }
            logger.info("found {} samples from {}, resolved {}, too far {}", decode.samples.size(), from, i, tooFar);
            decode.request.node.put(Command.SAMPLE_R);
        }
    }

    private boolean isFine(String value) {
        try {
            Entry entry = FileDB.MAPPER.readValue(value, Entry.class);
            return NameFilter.fineMatch(entry.name);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private boolean closeEnough(Node node, String hash) {
        String id = Generator.toHex(node.id.array());
        return id.substring(0, 3).equals(hash.substring(0, 3));
    }
}
