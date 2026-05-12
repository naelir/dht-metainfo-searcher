package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public interface IRequest {
    static Random RANDOM = new Random();

    void decode(BencodedDictionary map) throws IOException, CircularReferenceException;

    byte[] encode();

    ByteBuffer id();

    String method();

    default ByteBuffer nextTid() {
        byte[] b = new byte[8];
        RANDOM.nextBytes(b);
        return ByteBuffer.wrap(b);
    }

    ByteBuffer tid();
}
