package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class ResponseResolver {
    public static final Logger logger = LogManager.getLogger(ResponseResolver.class);
    private Data data;

    public ResponseResolver(Data data) {
        this.data = data;
    }

    private AnnouncePeerResponse resolve(AnnouncePeerRequest message, From from) {
        Token found = this.data.tokens.get(message.token);
        if (found != null && found.nodeId().equals(message.id)) {
            Integer remotePort = message.port;
            if (message.implied != 0) {
                remotePort = from.port;
            }
            Torrent torrent = this.data.torrents.get(message.infoHash);
            Node node = new Node(from.ip, remotePort, message.id);
            torrent.peers.add(node);
        }
        return new AnnouncePeerResponse(message.tid, this.data.myself);
    }

    private void resolve(AnnouncePeerResponse decode, From from) {
        // TODO Auto-generated method stub
    }

    public Optional<byte[]> resolve(BencodedDictionary map, From from) {
        try {
            String type = KRPCKeys.getType(map);
            if (KRPCKeys.QUERY.equals(type)) {
                String method = KRPCKeys.getQuery(map);
                Object decode = CommandDecoder.decode(map, method);
                return resolve(decode, from);
            } else {
                ByteBuffer tid = KRPCKeys.getTransaction(map);
                IRequest found = this.data.commandsSent.remove(new CommandId(tid, from.ip, from.port));
                if (found != null) {
                    Object decode = CommandDecoder.decode(map, found.method());
                    return resolve(decode, from);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private FindNodeResponse resolve(FindNodeRequest message, From from) {
        List<Node> nodeList = this.data.closest(message.target, Config.NUM_RETURN_NODES);
        ByteBuffer compact = CompactInfo.compactNodes(nodeList);
        return new FindNodeResponse(message.tid, this.data.myself, compact);
    }

    private Optional<byte[]> resolve(FindNodeResponse decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }

    private GetPeersResponse resolve(GetPeersRequest message, From from) {
        Token token = new Token(message.id, from.ip);
        this.data.tokens.put(token.token(), token);
        ByteBuffer infoHash = message.infoHash;
        Torrent torrent = this.data.torrents.get(infoHash);
        if (torrent != null && torrent.peers.size() > 0) {
            List<ByteBuffer> compact = CompactInfo.compactPeers(torrent.peers);
            return new GetPeersResponse1(message.tid, this.data.myself, token.token, compact);
        } else {
            List<Node> closest = this.data.closest(infoHash, Config.NUM_RETURN_NODES);
            Node node = this.data.nodes.get(message.id);
            ByteBuffer nodes = CompactInfo.compactNodes(closest);
            this.data.torrents.put(infoHash, new Torrent(infoHash, token.token, node));
            return new GetPeersResponse2(message.tid, this.data.myself, token.token, nodes);
        }
    }

    private Optional<byte[]> resolve(GetPeersResponse1 decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }

    private Optional<byte[]> resolve(GetPeersResponse2 decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }

    private Optional<byte[]> resolve(Object decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }

    private PingResponse resolve(PingRequest decode, From from) {
        return new PingResponse(decode.tid, this.data.myself);
    }

    private void resolve(PingResponse decode, From from) {
    }

    private Optional<byte[]> resolve(SampleInfoHashesRequest decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }

    private Optional<byte[]> resolve(SampleInfoHashesResponse decode, From from) {
        // TODO Auto-generated method stub
        return null;
    }
}
