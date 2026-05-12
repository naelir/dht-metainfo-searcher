package com.naelir.dht;

import java.nio.ByteBuffer;

public class AnnouncePeerResponse {
//    CommandId commandId;
    ByteBuffer id;
    public ByteBuffer tid;

    public AnnouncePeerResponse() {
        // TODO Auto-generated constructor stub
    }

    public AnnouncePeerResponse(ByteBuffer tid, ByteBuffer id) {
        super();
        this.tid = tid;
        this.id = id;
    }
}
