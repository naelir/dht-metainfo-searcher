package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
//announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;

public class AnnouncePeerRequest implements IRequest {
    ByteBuffer id;
    ByteBuffer infoHash;
    ByteBuffer token;
    Integer implied;
    Integer port;
    String method = "announce_peer";
    ByteBuffer tid;
    Node node;

    public AnnouncePeerRequest() {
        // TODO Auto-generated constructor stub
    }

    public AnnouncePeerRequest(ByteBuffer id, ByteBuffer infoHash, ByteBuffer token, int port, Node node) {
        this.id = id;
        this.infoHash = infoHash;
        this.token = token;
        this.port = port;
        this.node = node;
        this.tid = nextTid();
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary args = KRPCKeys.getArgs(map);
        this.id = KRPCKeys.getId(args);
        this.infoHash = KRPCKeys.getInfoHash(args);
        this.token = KRPCKeys.getToken(args);
        this.implied = KRPCKeys.getImpliedPort(args);
        this.port = KRPCKeys.getPort(args);
    }

    @Override
    public byte[] encode() {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(this.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(this.id.array()));
        args.put(KRPCKeys.INFO_HASH, new BencodedByteSequence(this.infoHash.array()));
        args.put(KRPCKeys.PORT, new BencodedInteger(this.port));
        args.put(KRPCKeys.TOKEN, new BencodedByteSequence(this.token.toString()));
        return BEncoder.bytes(query);
    }

    @Override
    public ByteBuffer id() {
        return this.id;
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public ByteBuffer tid() {
        return this.tid;
    }

    @Override
    public String toString() {
        return "AnnouncePeerRequest [id=" + Generator.toHex(this.id.array()) + ", infoHash="
                + Generator.toHex(this.infoHash.array()) + ", implied=" + this.implied + ", port=" + this.port + "]";
    }
}
