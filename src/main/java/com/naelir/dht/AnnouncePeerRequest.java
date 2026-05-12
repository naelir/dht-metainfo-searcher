package com.naelir.dht;

import java.nio.ByteBuffer;
//announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}

public class AnnouncePeerRequest implements IRequest {
    ByteBuffer tid;
//    CommandId commandId;
    ByteBuffer id;
    ByteBuffer infoHash;
    ByteBuffer token;
    int implied;
    int port;
    String method = "announce_peer";

    public AnnouncePeerRequest() {
        // TODO Auto-generated constructor stub
    }

    public AnnouncePeerRequest(ByteBuffer tid, ByteBuffer id, ByteBuffer infoHash, ByteBuffer token, int port) {
        super();
        this.tid = tid;
        this.id = id;
        this.infoHash = infoHash;
        this.token = token;
        this.port = port;
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
