package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

/*
{
    "a":
    {
        "id": <20 byte id of sending node (string)>,
        "target": <20 byte ID for nodes>,
    },
    "t": <transaction-id (string)>,
    "y": "q",
    "q": "sample_infohashes"
}
*/
public class SampleInfoHashesRequest implements IRequest {
    ByteBuffer tid;
    String type = KRPCKeys.QUERY;
    String method = "sample_infohashes";
    ByteBuffer id;
    ByteBuffer target;
    Node node;

    public SampleInfoHashesRequest() {
        // TODO Auto-generated constructor stub
    }

    public SampleInfoHashesRequest(ByteBuffer id, ByteBuffer target, Node node) {
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
        args.put(KRPCKeys.ID, new BencodedByteSequence(this.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(this.target.array()));
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
        return "SampleInfoHashesRequest [id=" + Generator.toHex(this.id.array()) + ", target="
                + Generator.toHex(this.target.array()) + "]";
    }
}
