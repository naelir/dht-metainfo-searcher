package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class FindNodeResponse implements IResponse {
    ByteBuffer id;
    List<Node> nodes;
    ByteBuffer tid;
    FindNodeRequest request;

    public FindNodeResponse(ByteBuffer tid2, ByteBuffer myself, List<Node> nodes, FindNodeRequest message) {
        this.tid = tid2;
        this.id = myself;
        this.nodes = nodes;
        this.request = message;
    }

    public FindNodeResponse(IRequest request) {
        this.request = (FindNodeRequest) request;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary rsp = KRPCKeys.getResponse(map);
        this.id = KRPCKeys.getId(rsp);
        this.nodes = CompactInfo.expandNodes(KRPCKeys.getNodes(rsp));
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(this.id.array()));
        ByteBuffer n = CompactInfo.compactNodes(this.nodes);
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(n.array()));
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
        return "FindNodeResponse [id=" + Generator.toHex(this.id.array()) + ", nodes=" + this.nodes.size() + "]";
    }
}
