package com.naelir.dht;

import java.nio.ByteBuffer;

public abstract class GetPeersResponse {
    ByteBuffer id;
    ByteBuffer token;
    ByteBuffer tid;

    public GetPeersResponse(ByteBuffer tid, ByteBuffer id, ByteBuffer token) {
        super();
        this.tid = tid;
        this.id = id;
        this.token = token;
    }
}
