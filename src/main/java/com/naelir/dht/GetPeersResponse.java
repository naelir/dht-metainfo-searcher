package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedList;

public class GetPeersResponse implements IResponse {
    public static final Logger logger = LogManager.getLogger(GetPeersResponse.class);
    ByteBuffer id;
    ByteBuffer token;
    ByteBuffer tid;
    List<Node> peers;
    IRequest request;
    List<Node> nodes;

    public GetPeersResponse(ByteBuffer tid, ByteBuffer myself, ByteBuffer token, List<Node> nodes, List<Node> peers,
            GetPeersRequest message) {
        this.tid = tid;
        this.id = myself;
        this.token = token;
        this.peers = peers;
        this.request = message;
        this.nodes = nodes;
    }

    public GetPeersResponse(IRequest request) {
        this.request = request;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        BencodedDictionary rsp = KRPCKeys.getResponse(map);
        this.tid = KRPCKeys.getTransaction(map);
        this.id = KRPCKeys.getId(rsp);
        this.token = KRPCKeys.getToken(rsp);
        this.nodes = CompactInfo.expandNodes(KRPCKeys.getNodes(rsp));
        List<ByteBuffer> values = KRPCKeys.getValues(rsp);
        this.peers = CompactInfo.expandPeers(values);
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(this.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.TOKEN), new BencodedByteSequence(this.token.array()));
        ByteBuffer found = CompactInfo.compactNodes(this.nodes);
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(found.array()));
        if (this.peers.isEmpty() == false) {
            BencodedList values = new BencodedList();
            List<ByteBuffer> compact = CompactInfo.compactPeers(this.peers);
            for (ByteBuffer peer : compact) {
                values.add(new BencodedByteSequence(peer.array()));
            }
            args.put(new BencodedByteSequence(KRPCKeys.VALUES), values);
        }
        return BEncoder.bytes(query);
    }

    @Override
    public ByteBuffer id() {
        return this.id;
    }

    @Override
    public IRequest request() {
        return this.request;
    }

    @Override
    public String toString() {
        return "GetPeersResponse [id=" + Generator.toHex(this.id.array()) + ", peers=" + this.peers.size() + ", nodes="
                + this.nodes.size() + "]";
    }
}
