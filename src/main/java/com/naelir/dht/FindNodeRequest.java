package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class FindNodeRequest implements IRequest {
    ByteBuffer id;
    ByteBuffer target;
    String method = "find_node";
    ByteBuffer tid;
    Node node;

    public FindNodeRequest() {
        // TODO Auto-generated constructor stub
    }

    public FindNodeRequest(ByteBuffer id, ByteBuffer target, Node node) {
        this.node = node;
        this.tid = nextTid();
        this.id = id;
        this.target = target;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary args = KRPCKeys.getArgs(map);
        this.id = KRPCKeys.getId(args);
        this.target = KRPCKeys.getTarget(args);
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(this.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(this.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(this.target.array()));
        return BEncoder.bytes(query);
    }

    @Override
    public ByteBuffer id() {
        return this.id;
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public ByteBuffer tid() {
        return this.tid;
    }

    @Override
    public String toString() {
        return "FindNodeRequest [id=" + Generator.toHex(this.id.array()) + ", target="
                + Generator.toHex(this.target.array()) + "]";
    }
}
