package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.List;

public class GetPeersResponse1 extends GetPeersResponse implements IResponse {
//    CommandId commandId;
    List<ByteBuffer> values;
    IRequest request;

    public GetPeersResponse1(ByteBuffer tid, ByteBuffer id, ByteBuffer token, List<ByteBuffer> values,
            IRequest request) {
        super(tid, id, token);
        this.values = values;
        this.request = request;
    }

    @Override
    public IRequest request() {
        return this.request;
    }
}
