package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;

public final class Error implements IResponse {
    Integer status;
    String message;
    public ByteBuffer tid;
    private IRequest found;

    public Error(Integer status, String message, ByteBuffer tid) {
        this.status = status;
        this.message = message;
        this.tid = tid;
    }

    public Error(IRequest found) {
        this.found = found;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedList list = KRPCKeys.getError(map);
        BencodedInteger code = (BencodedInteger) list.get(0);
        BencodedByteSequence m = (BencodedByteSequence) list.get(1);
        this.status = code != null ? (int) code.getValue() : null;
        this.message = m.toUTF8String();
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        BencodedList list = new BencodedList();
        list.add(new BencodedInteger(this.status));
        list.add(new BencodedByteSequence(this.message));
        query.put(KRPCKeys.TYPE, new BencodedByteSequence(KRPCKeys.ERROR));
        query.put(KRPCKeys.ERROR, list);
        return BEncoder.bytes(query);
    }

    @Override
    public ByteBuffer id() {
        return ByteBuffer.allocate(0);
    }

    @Override
    public IRequest request() {
        return this.found;
    }

    @Override
    public String toString() {
        return "Error [status=" + this.status + ", message=" + this.message + "]";
    }
}
