package com.naelir.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

/*
{
    "r":
    {
        "id": <20 byte id of sending node (string)>,
        "interval": <the subset refresh interval in seconds (integer)>,
        "nodes": <nodes close to 'target'>,
        "num": <number of infohashes in storage (integer)>,
        "samples": <subset of stored infohashes, N × 20 bytes (string)>
    },
    "t": <transaction-id (string)>,
    "y": "r"
}
*/
public class SampleInfoHashesResponse implements IResponse {
    public static SampleInfoHashesResponse of(BencodedDictionary map) {
        SampleInfoHashesResponse name = new SampleInfoHashesResponse();
        name.tid = KRPCKeys.getTransaction(map);
        name.type = KRPCKeys.getType(map);
        BencodedDictionary args = KRPCKeys.getResponse(map);
        if (args != null) {
            name.id = KRPCKeys.getId(args);
            name.nodes = KRPCKeys.getNodes(args);
        }
        return name;
    }

    ByteBuffer tid;
    ByteBuffer id;
    String type = "sample_infohashes";
    Integer interval;
    ByteBuffer nodes;
    Integer num;
    ByteBuffer samples;
    private IRequest request;

    public SampleInfoHashesResponse() {
        // TODO Auto-generated constructor stub
    }

    public SampleInfoHashesResponse(ByteBuffer tid, ByteBuffer id, Integer interval, ByteBuffer nodes, Integer num,
            ByteBuffer samples, IRequest request) {
        super();
        this.tid = tid;
        this.id = id;
        this.interval = interval;
        this.nodes = nodes;
        this.num = num;
        this.samples = samples;
        this.request = request;
    }

    public byte[] encode() throws IOException, CircularReferenceException {
        BencodedDictionary answer = new BencodedDictionary();
        answer.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        BencodedDictionary args = new BencodedDictionary();
        answer.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        answer.put(new BencodedByteSequence(KRPCKeys.RESPONSE), args);
        args.put(KRPCKeys.ID, new BencodedByteSequence(this.id.array()));
        if (this.nodes != null) {
            answer.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(this.nodes.array()));
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        answer.writeObject(os);
        return os.toByteArray();
    }

    @Override
    public IRequest request() {
        return this.request;
    }
}
