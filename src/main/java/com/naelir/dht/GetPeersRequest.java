package com.naelir.dht;

import java.nio.ByteBuffer;
//get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}

public class GetPeersRequest implements IRequest {
//    CommandId commandId;
    ByteBuffer id;
    ByteBuffer infoHash;
    ByteBuffer tid;
    String method = "get_peers";

    public GetPeersRequest() {
        // TODO Auto-generated constructor stub
    }

    public GetPeersRequest(ByteBuffer tid, ByteBuffer id, ByteBuffer infoHash) {
        super();
        this.tid = tid;
        this.id = id;
        this.infoHash = infoHash;
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
