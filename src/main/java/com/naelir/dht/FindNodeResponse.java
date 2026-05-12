package com.naelir.dht;

import java.nio.ByteBuffer;

public class FindNodeResponse {
//    CommandId commandId;
    ByteBuffer id;
    ByteBuffer nodes;
    ByteBuffer tid;

    public FindNodeResponse() {
        // TODO Auto-generated constructor stub
    }

    public FindNodeResponse(ByteBuffer tid, ByteBuffer id, ByteBuffer nodes) {
        super();
        this.tid = tid;
        this.id = id;
        this.nodes = nodes;
    }
}
