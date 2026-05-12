package com.naelir.dht;

import java.nio.ByteBuffer;

//ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
public class PingRequest implements IRequest {
//    CommandId commandId;
    ByteBuffer from;
    ByteBuffer tid;
    String method = "ping";

    @Override
    public String method() {
        return this.method;
    }
}
