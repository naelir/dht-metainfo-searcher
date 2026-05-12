package com.naelir.dht;

import java.nio.ByteBuffer;

//ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
public class PingRequest implements IRequest {
//    CommandId commandId;
    ByteBuffer from;
    ByteBuffer tid;
    String method = "ping";

    public PingRequest() {
        // TODO Auto-generated constructor stub
    }

    public PingRequest(ByteBuffer tid, ByteBuffer from) {
        super();
        this.tid = tid;
        this.from = from;
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public ByteBuffer tid() {
        return this.tid;
    }
}
