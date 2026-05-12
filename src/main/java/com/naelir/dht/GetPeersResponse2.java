package com.naelir.dht;

import java.nio.ByteBuffer;

public class GetPeersResponse2 extends GetPeersResponse implements IResponse {
    ByteBuffer nodes;
    IRequest request;

    public GetPeersResponse2(ByteBuffer tid, ByteBuffer id, ByteBuffer token, ByteBuffer nodes, IRequest request) {
        super(tid, id, token);
        this.nodes = nodes;
        this.request = request;
    }

    @Override
    public IRequest request() {
        return this.request;
    }
}
