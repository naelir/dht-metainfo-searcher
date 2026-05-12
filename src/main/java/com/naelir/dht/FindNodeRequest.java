package com.naelir.dht;

import java.nio.ByteBuffer;

public class FindNodeRequest implements IRequest {
    ByteBuffer id;
    ByteBuffer target;
    String method = "find_node";
    ByteBuffer tid;

    public FindNodeRequest() {
        // TODO Auto-generated constructor stub
    }

    public FindNodeRequest(ByteBuffer tid, ByteBuffer id, ByteBuffer target) {
        this.tid = tid;
        this.id = id;
        this.target = target;
    }

    @Override
    public String method() {
        return this.method;
    }
}
