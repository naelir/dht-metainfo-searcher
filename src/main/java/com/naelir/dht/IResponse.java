package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public interface IResponse {
    void decode(BencodedDictionary map) throws IOException, CircularReferenceException;

    byte[] encode();

    ByteBuffer id();

    IRequest request();
}
