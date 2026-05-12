package com.naelir.dht;

import java.io.IOException;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class CommandDecoder {
    public static Object decodeError(BencodedDictionary map, IRequest found)
            throws Exception, IOException, CircularReferenceException {
        Error error = new Error(found);
        error.decode(map);
        return error;
    }

    public static Object decodeRequest(BencodedDictionary map)
            throws Exception, IOException, CircularReferenceException {
        String method = KRPCKeys.getQuery(map);
        return switch (method) {
        case KRPCKeys.Commands.PING -> {
            PingRequest pr = new PingRequest();
            pr.decode(map);
            yield pr;
        }
        case KRPCKeys.Commands.ANNOUNCE_PEER -> {
            AnnouncePeerRequest apr = new AnnouncePeerRequest();
            apr.decode(map);
            yield apr;
        }
        case KRPCKeys.Commands.FIND_NODE -> {
            FindNodeRequest fn = new FindNodeRequest();
            fn.decode(map);
            yield fn;
        }
        case KRPCKeys.Commands.GET_PEERS -> {
            GetPeersRequest gp = new GetPeersRequest();
            gp.decode(map);
            yield gp;
        }
        case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
            SampleInfoHashesRequest sih = new SampleInfoHashesRequest();
            sih.decode(map);
            yield sih;
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + method);
        };
    }

    public static Object decodeResponse(BencodedDictionary map, IRequest found)
            throws Exception, IOException, CircularReferenceException {
        return switch (found.method()) {
        case KRPCKeys.Commands.PING -> {
            PingResponse pr = new PingResponse(found);
            pr.decode(map);
            yield pr;
        }
        case KRPCKeys.Commands.ANNOUNCE_PEER -> {
            AnnouncePeerResponse apr = new AnnouncePeerResponse(found);
            apr.decode(map);
            yield apr;
        }
        case KRPCKeys.Commands.FIND_NODE -> {
            FindNodeResponse fn = new FindNodeResponse(found);
            fn.decode(map);
            yield fn;
        }
        case KRPCKeys.Commands.GET_PEERS -> {
            GetPeersResponse gp = new GetPeersResponse(found);
            gp.decode(map);
            yield gp;
        }
        case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
            SampleInfoHashesResponse sihr = new SampleInfoHashesResponse(found);
            sihr.decode(map);
            yield sihr;
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + found);
        };
    }
}
