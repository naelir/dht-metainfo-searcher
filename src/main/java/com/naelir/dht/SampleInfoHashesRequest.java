package com.naelir.dht;

import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

/*
{
    "a":
    {
        "id": <20 byte id of sending node (string)>,
        "target": <20 byte ID for nodes>,
    },
    "t": <transaction-id (string)>,
    "y": "q",
    "q": "sample_infohashes"
}
*/
public class SampleInfoHashesRequest implements IRequest {
    public static SampleInfoHashesRequest of(BencodedDictionary map) {
        SampleInfoHashesRequest request = new SampleInfoHashesRequest();
        request.tid = KRPCKeys.getTransaction(map);
        BencodedDictionary args = KRPCKeys.getArgs(map);
        if (args != null) {
            request.id = KRPCKeys.getId(args);
            request.target = KRPCKeys.getTarget(args);
        }
        return request;
    }

    ByteBuffer tid;
    String type = KRPCKeys.QUERY;
    String method = "sample_infohashes";
    ByteBuffer id;
    ByteBuffer target;

    @Override
    public String method() {
        return this.method;
    }
}
