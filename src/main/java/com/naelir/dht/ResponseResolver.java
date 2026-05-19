package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naeir.bt.Torrent;
import com.naelir.dht.Node.Command;

public class ResponseResolver {
    public static final Logger logger = LogManager.getLogger(ResponseResolver.class);
    private Data data;

    public ResponseResolver(Data data) {
        this.data = data;
    }

    private void logFrom(Object decode, From from) {
        logger.info("{}, from {}, port {}", decode.getClass().getSimpleName(), Arrays.toString(from.ip), from.port);
    }

    private Optional<byte[]> optional(byte[] encode) {
        if (encode != null && encode.length > 0)
            return Optional.of(encode);
        else
            return Optional.empty();
    }

    private AnnouncePeerResponse resolve(AnnouncePeerRequest message, From from) {
        Node found = this.data.tokensSent.get(message.token);
        ByteBuffer hashed = Token.token(from.ip);
        if (found != null && message.token.equals(hashed)) {
            Integer remotePort = message.port;
            if (message.implied != 0) {
                remotePort = from.port;
            }
            Node node = new Node(from.ip, remotePort, message.id);
            Torrent newOne = new Torrent(message.infoHash);
            newOne.peers().add(node);
            Torrent previous = this.data.torrents.putIfAbsent(message.infoHash, newOne);
            if (previous != null) {
                previous.peers().add(node);
            }
        }
        return new AnnouncePeerResponse(message.tid, this.data.myself, message);
    }

    private void resolve(AnnouncePeerResponse decode, From from) {
        Node node = this.data.table.getNode(decode.id);
        if (node != null) {
            Query command = node.query(Command.ANNOUNCE);
            command.responded();
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
                IRequest found = this.data.sent.remove(new CommandId(tid, from.ip, from.port));
                if (found != null) {
                    Object decode = CommandDecoder.decodeResponse(map, found);
                    logFrom(decode, from);
                    return resolve(decode, from);
                }
            } else {
                ByteBuffer tid = KRPCKeys.getTransaction(map);
                IRequest found = this.data.sent.remove(new CommandId(tid, from.ip, from.port));
                if (found != null) {
                    Object decode = CommandDecoder.decodeError(map, found);
                    logFrom(decode, from);
                    return resolve(decode, from);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private FindNodeResponse resolve(FindNodeRequest message, From from) {
        List<Node> nodeList = this.data.table.closest(message.target);
        ByteBuffer compact = CompactInfo.compactNodes(nodeList);
        return new FindNodeResponse(message.tid, this.data.myself, compact, message);
    }

    private Optional<byte[]> resolve(FindNodeResponse decode, From from) {
        RoutingTable buckets = this.data.table;
        Node node = buckets.getNode(decode.id);
        if (node != null) {
            node.query(Command.FIND_NODE).responded();
        }
        ByteBuffer nodes = decode.nodes;
        List<Node> expandNodes = CompactInfo.expandNodes(nodes);
        logger.info("found {} nodes from {}", expandNodes.size(), from.ip);
        for (Node en : expandNodes) {
            this.data.table.insertNode(en);
        }
        return Optional.empty();
    }

    private GetPeersResponse resolve(GetPeersRequest message, From from) {
        Token token = new Token(from.ip);
        ByteBuffer infoHash = message.infoHash;
        Torrent torrent = this.data.torrents.get(infoHash);
        Node node = this.data.table.getNode(message.id);
        if (node != null) {
            this.data.tokensSent.put(token.value, node);
        } else {
            Node newOne = new Node(from.ip, from.port, message.id);
            this.data.tokensSent.put(token.value, newOne);
        }
        if (torrent != null && torrent.peers().size() > 0) {
            List<ByteBuffer> compact = CompactInfo.compactPeers(torrent.peers());
            return new GetPeersResponse1(message.tid, this.data.myself, token.value, compact, message);
        } else {
            List<Node> closest = this.data.table.closest(infoHash);
            ByteBuffer nodes = CompactInfo.compactNodes(closest);
            this.data.torrents.put(infoHash, new Torrent(infoHash));
            return new GetPeersResponse2(message.tid, this.data.myself, token.value, nodes, message);
        }
    }

    private Optional<byte[]> resolve(GetPeersResponse1 decode, From from) {
        List<ByteBuffer> list = decode.values;
        IRequest req = decode.request;
        if (req instanceof GetPeersRequest gpr) {
            Node node = this.data.table.getNode(decode.id);
            if (node != null) {
                this.data.tokensReceived.put(decode.token, node);
            } else {
                this.data.tokensReceived.put(decode.token, new Node(from.ip, from.port, decode.id));
            }
            List<Node> expand = CompactInfo.expand(list);
            Torrent torrent = this.data.torrents.get(gpr.infoHash);
            if (torrent != null) {
                torrent.peers().addAll(expand);
            } else {
                this.data.torrents.put(gpr.infoHash, new Torrent(gpr.infoHash, expand));
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> resolve(GetPeersResponse2 decode, From from) {
        ByteBuffer list = decode.nodes;
        IRequest req = decode.request;
        if (req instanceof GetPeersRequest) {
            Node found = this.data.table.getNode(decode.id);
            if (found != null) {
                this.data.tokensReceived.put(decode.token, found);
            } else {
                this.data.tokensReceived.put(decode.token, new Node(from.ip, from.port, decode.id));
            }
            List<Node> expand = CompactInfo.expandNodes(list);
            for (Node node : expand) {
                this.data.table.insertNode(node);
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> resolve(IRequest decode, From from) {
        if (decode instanceof AnnouncePeerRequest apr) {
            AnnouncePeerResponse r = resolve(apr, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof GetPeersRequest gpr) {
            GetPeersResponse r = resolve(gpr, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof FindNodeRequest fnr) {
            FindNodeResponse r = resolve(fnr, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof PingRequest pr) {
            PingResponse r = resolve(pr, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else if (decode instanceof SampleInfoHashesRequest sihr) {
            SampleInfoHashesResponse r = resolve(sihr, from);
            byte[] encode = BEncoder.encode(r);
            return optional(encode);
        } else
            return Optional.empty();
    }

    private Optional<byte[]> resolve(IResponse decode, From from) {
        if (decode instanceof AnnouncePeerResponse apr) {
            resolve(apr, from);
        } else if (decode instanceof GetPeersResponse1 gpr) {
            resolve(gpr, from);
        } else if (decode instanceof GetPeersResponse2 fnr) {
            resolve(fnr, from);
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
        Node node = this.data.table.getNode(decode.id);
        if (node != null) {
            node.query(Command.PING).responded(TimeUnit.MINUTES, 15);
        }
    }

    private SampleInfoHashesResponse resolve(SampleInfoHashesRequest decode, From from) {
        ByteBuffer nodes = ByteBuffer.wrap(new byte[0]);
        return new SampleInfoHashesResponse(decode.tid, this.data.myself, 3600, nodes, 0, nodes, decode);
    }

    private void resolve(SampleInfoHashesResponse decode, From from) {//
        if (decode.samples != null && decode.samples.array().length > 0) {
            List<ByteBuffer> expandHashes = CompactInfo.expandHashes(decode.samples);
            logger.info("found torrent hashes {}", expandHashes.size());
            for (ByteBuffer hash : expandHashes) {
                this.data.torrents.put(hash, new Torrent(hash));
            }
            Node node = this.data.table.getNode(decode.id);
            if (node != null) {
                node.query(Command.SAMPLE).responded(TimeUnit.SECONDS, decode.interval);
            }
        }
    }
}
