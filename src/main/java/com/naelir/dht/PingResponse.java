package com.naelir.dht;

import java.nio.ByteBuffer;

public class PingResponse {
    public ByteBuffer tid;
    public ByteBuffer id;

    public PingResponse() {
        // TODO Auto-generated constructor stub
    }

    public PingResponse(ByteBuffer tid, ByteBuffer id) {
        super();
        this.tid = tid;
        this.id = id;
    }
}
