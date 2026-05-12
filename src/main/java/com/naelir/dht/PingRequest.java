package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

//ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
public class PingRequest implements IRequest {
    ByteBuffer tid;
    String method = "ping";
    ByteBuffer id;
    Node node;

    public PingRequest() {
        // TODO Auto-generated constructor stub
    }

    public PingRequest(ByteBuffer id, Node node) {
        this.node = node;
        this.tid = nextTid();
        this.id = id;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary args = KRPCKeys.getArgs(map);
        this.id = KRPCKeys.getId(args);
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(this.method));
        args.put(KRPCKeys.ID, new BencodedByteSequence(this.id.array()));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
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
        return "PingRequest [id=" + Generator.toHex(this.id.array()) + "]";
    }
}
