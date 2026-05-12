package com.naelir.dht;

import java.nio.ByteBuffer;

public class GetPeersResponse2 extends GetPeersResponse {
    ByteBuffer nodes;

    public GetPeersResponse2(ByteBuffer tid, ByteBuffer id, ByteBuffer token, ByteBuffer nodes) {
        super(tid, id, token);
        this.nodes = nodes;
    }
}
