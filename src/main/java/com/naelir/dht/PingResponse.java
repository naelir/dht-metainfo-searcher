package com.naelir.dht;

import java.nio.ByteBuffer;

public class PingResponse implements IResponse {
    public ByteBuffer tid;
    public ByteBuffer id;
    IRequest request;

    public PingResponse() {
        // TODO Auto-generated constructor stub
    }

    public PingResponse(ByteBuffer tid, ByteBuffer id, IRequest request) {
        super();
        this.tid = tid;
        this.id = id;
        this.request = request;
    }

    @Override
    public IRequest request() {
        return this.request;
    }
}
