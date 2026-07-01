package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.bt.IpRangeFilter;
import com.naelir.bt.Torrent;

public class ResponseResolver {
    public static final Logger logger = LogManager.getLogger(ResponseResolver.class);
    private Data data;

    public ResponseResolver(Data data) {
        this.data = data;
    }

    private Object forAddress(From from) {
        try {
            return InetAddress.getByAddress(from.ip);
        } catch (UnknownHostException e) {
            return Arrays.toString(from.ip);
        }
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
            if (message.implied != 0) {
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
            data.queryStats.get(apr.node, Command.ANNOUNCE).ifPresent(q -> {
                q.setResponded();
            });
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
                IRequest found = this.data.sent.remove(tid);
                if (found != null) {
                    Object decode = CommandDecoder.decodeResponse(map, found);
                    logFrom(decode, from);
                    return resolve(decode, from);
                }
            } else {
                ByteBuffer tid = KRPCKeys.getTransaction(map);
                if (tid != null) {
                    IRequest found = this.data.sent.remove(tid);
                    if (found != null) {
                        Object decode = CommandDecoder.decodeError(map, found);
                        logFrom(decode, from);
                        return resolve(decode, from);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private FindNodeResponse resolve(FindNodeRequest message, From from) {
        List<Node> nodes = this.data.table.closest(message.target);
        logger.info("request to myself {} resolved, found {} close nodes", Generator.toHex(message.target.array()),
                nodes.size());
        return new FindNodeResponse(message.tid, this.data.myself, nodes, message);
    }

    private Optional<byte[]> resolve(FindNodeResponse decode, From from) {
        data.queryStats.get(decode.request.node, Command.ANNOUNCE).ifPresent(q -> {
            q.setResponded();
        });

        int size = data.table.size();
        if (data.maxNodes > size) {
            for (Node node : decode.nodes) {
                if (IpRangeFilter.isDenied(node.ip) == false || size < 5) {
                    this.data.table.insert(node);
                }
            }
        }
        return Optional.empty();
    }

    private Object resolve(GetPeersRequest message, From from) {
        Token token = new Token(from.ip);
        ByteBuffer infoHash = message.infoHash;
        String hex = Generator.toHex(infoHash.array());
        Torrent torrent = this.data.torrents.get(hex);
        this.data.tokensSent.put(token.value, new Node(from.ip, from.port, message.id));
        List<Node> closest = this.data.table.closest(infoHash);
        if (torrent != null) {
            Deque<Node> peers = torrent.peers();
            List<Node> name = new ArrayList<>(peers);
            return new GetPeersResponse(message.tid, this.data.myself, token.value, name, closest, message);
        } else
            return new GetPeersResponse(message.tid, this.data.myself, token.value, Collections.emptyList(), closest,
                    message);
    }

    private Optional<byte[]> resolve(GetPeersResponse decode, From from) {
        IRequest req = decode.request;
        if (req instanceof GetPeersRequest gpr) {
            if (decode.token != null) {
                this.data.tokensReceived.put(decode.token, gpr.node);
            }
            String hex = Generator.toHex(gpr.infoHash.array());
            Torrent torrent = this.data.torrents.get(hex);
            if (torrent != null && torrent.infoHash() != null && torrent.infoHash().length() > 0 && torrent.meta() == null) {
                List<Node> list = decode.peers.stream().filter(e -> IpRangeFilter.isDenied(e.ip) == false).toList();
                if (list.isEmpty() == false) {
                    logger.info("found {} peers for {}, applicable {}", decode.peers.size(), hex, list.size());
                    this.data.pingTasks.add(new PingPeersTorrentTask(list, torrent));
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
            FindNodeResponse r = resolve(fnr, from);
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
        data.queryStats.get(decode.request.node, Command.PING).ifPresent(q -> {
            q.responded(TimeUnit.MINUTES, 15);
        });
    }

    private IResponse resolve(SampleInfoHashesRequest decode, From from) {
        List<Node> nodes = this.data.table.closest(decode.target, 8);
        return new SampleInfoHashesResponse(decode.tid, this.data.myself, 3600, nodes, 0, List.of(), decode);
    }

    private void resolve(SampleInfoHashesResponse decode, From from) {//
        if (decode.samples.isEmpty() == false) {
            int i = 0;
            for (String hash : decode.samples) {
                Torrent torrent = new Torrent(hash);
                Torrent prev = this.data.torrents.putIfAbsent(hash, torrent);
                if (prev == null) {
                    i++;
                }
            }
            logger.info("found {} samples from {}, inserted new {}", decode.samples.size(), from, i);
            this.data.samples.offer(decode);

            data.queryStats.get(decode.request.node, Command.SAMPLE).ifPresent(q -> {
                q.responded(TimeUnit.SECONDS, decode.interval);
            });
            
            if (this.data.table.nodes().size() < data.maxNodes) {
                for (Node node : decode.nodes) {
                    this.data.table.insert(node);
                }
            }
        }
    }
}
