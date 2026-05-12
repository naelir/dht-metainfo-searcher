package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class AnnouncePeerResponse implements IResponse {
    ByteBuffer id;
    public ByteBuffer tid;
    IRequest request;

    public AnnouncePeerResponse(ByteBuffer tid, ByteBuffer myself, AnnouncePeerRequest request) {
        this.tid = tid;
        this.id = myself;
        this.request = request;
    }

    public AnnouncePeerResponse(IRequest request) {
        this.request = request;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary rsp = KRPCKeys.getResponse(map);
        this.id = KRPCKeys.getId(rsp);
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(this.id.array()));
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
        return "AnnouncePeerResponse [id=" + Generator.toHex(this.id.array()) + "]";
    }
}
