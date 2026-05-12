package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
//get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class GetPeersRequest implements IRequest {
    ByteBuffer id;
    ByteBuffer infoHash;
    ByteBuffer tid;
    String method = "get_peers";
    Node node;

    public GetPeersRequest() {
        // TODO Auto-generated constructor stub
    }

    public GetPeersRequest(ByteBuffer id, ByteBuffer infoHash, Node node) {
        this.node = node;
        this.tid = nextTid();
        this.id = id;
        this.infoHash = infoHash;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary args = KRPCKeys.getArgs(map);
        this.id = KRPCKeys.getId(args);
        this.infoHash = KRPCKeys.getInfoHash(args);
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
        args.put(KRPCKeys.INFO_HASH, new BencodedByteSequence(this.infoHash.array()));
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
        return "GetPeersRequest [id=" + Generator.toHex(this.id.array()) + ", infoHash="
                + Generator.toHex(this.infoHash.array()) + "]";
    }
}
