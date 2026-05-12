package com.naelir.dht;

import java.nio.ByteBuffer;

public class AnnouncePeerResponse implements IResponse {
//    CommandId commandId;
    ByteBuffer id;
    public ByteBuffer tid;
    private IRequest request;

    public AnnouncePeerResponse() {
        // TODO Auto-generated constructor stub
    }

    public AnnouncePeerResponse(ByteBuffer tid, ByteBuffer id, IRequest request) {
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
