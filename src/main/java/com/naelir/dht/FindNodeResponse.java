package com.naelir.dht;

import java.nio.ByteBuffer;

public class FindNodeResponse implements IResponse {
//    CommandId commandId;
    ByteBuffer id;
    ByteBuffer nodes;
    ByteBuffer tid;
    private IRequest request;

    public FindNodeResponse() {
        // TODO Auto-generated constructor stub
    }

    public FindNodeResponse(ByteBuffer tid, ByteBuffer id, ByteBuffer nodes, IRequest request) {
        super();
        this.tid = tid;
        this.id = id;
        this.nodes = nodes;
        this.request = request;
    }

    @Override
    public IRequest request() {
        return this.request;
    }
}
