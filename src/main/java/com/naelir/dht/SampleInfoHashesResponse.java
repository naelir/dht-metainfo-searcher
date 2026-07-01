package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

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
    ByteBuffer tid;
    ByteBuffer id;
    String type = "sample_infohashes";
    Integer interval;
    List<Node> nodes;
    Integer num;
    public List<String> samples;
    SampleInfoHashesRequest request;

    public SampleInfoHashesResponse(ByteBuffer tid, ByteBuffer myself, int interval, List<Node> nodes, int num,
            List<String> of, SampleInfoHashesRequest decode) {
        this.tid = tid;
        this.id = myself;
        this.interval = interval;
        this.nodes = nodes;
        this.num = num;
        this.samples = of;
        this.request = decode;
    }

    public SampleInfoHashesResponse(IRequest request) {
        this.request = (SampleInfoHashesRequest) request;
    }

    @Override
    public void decode(BencodedDictionary map) throws IOException, CircularReferenceException {
        this.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary rsp = KRPCKeys.getResponse(map);
        this.id = KRPCKeys.getId(rsp);
        ByteBuffer n = KRPCKeys.getNodes(rsp);
        this.nodes = CompactInfo.expandNodes(n);
        this.interval = KRPCKeys.getInterval(rsp);
        this.num = KRPCKeys.getNum(rsp);
        this.samples = CompactInfo.expandHashes(KRPCKeys.getSamples(rsp));
    }

    @Override
    public byte[] encode() {
        BencodedDictionary answer = new BencodedDictionary();
        answer.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(this.tid.array()));
        BencodedDictionary args = new BencodedDictionary();
        answer.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        answer.put(new BencodedByteSequence(KRPCKeys.RESPONSE), args);
        args.put(KRPCKeys.ID, new BencodedByteSequence(this.id.array()));
        ByteBuffer compactNodes = CompactInfo.compactNodes(this.nodes);
        answer.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(compactNodes.array()));
        ByteBuffer hashes = CompactInfo.compactHashes(this.samples);
        answer.put(new BencodedByteSequence(KRPCKeys.SAMPLES), new BencodedByteSequence(hashes.array()));
        return BEncoder.bytes(answer);
    }

    @Override
    public ByteBuffer id() {
        return this.id;
    }

    @Override
    public IRequest request() {
        return this.request;
    }

    @Override
    public String toString() {
        return "SampleInfoHashesResponse [id=" + Generator.toHex(this.id.array()) + ", interval=" + this.interval
                + ", nodes=" + this.nodes.size() + ", num=" + this.num + ", samples=" + this.samples + "]";
    }
}
