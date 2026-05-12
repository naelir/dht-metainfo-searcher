package com.naelir.dht;

import java.nio.ByteBuffer;
//get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}

public class GetPeersRequest implements IRequest {
//    CommandId commandId;
    ByteBuffer id;
    ByteBuffer infoHash;
    ByteBuffer tid;
    String method = "get_peers";

    @Override
    public String method() {
        return this.method;
    }
}
