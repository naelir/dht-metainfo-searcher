package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.List;

public class GetPeersResponse1 extends GetPeersResponse {
//    CommandId commandId;
    List<ByteBuffer> values;

    public GetPeersResponse1(ByteBuffer tid, ByteBuffer id, ByteBuffer token, List<ByteBuffer> values) {
        super(tid, id, token);
        this.values = values;
    }
}
