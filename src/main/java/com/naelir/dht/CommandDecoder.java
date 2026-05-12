package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;
import com.github.cdefgah.bencoder4j.model.BencodedObject;

public class CommandDecoder {
    public static Object decodeError(BencodedDictionary map, IRequest found)
            throws Exception, IOException, CircularReferenceException {
        Error error = new Error();
        error.tid = KRPCKeys.getTransaction(map);
        BencodedList list = KRPCKeys.getError(map);
        BencodedInteger code = (BencodedInteger) list.get(0);
        BencodedByteSequence message = (BencodedByteSequence) list.get(1);
        error.status = code != null ? (int) code.getValue() : null;
        error.message = message.toUTF8String();
        return error;
    }

    public static Object decodeRequest(BencodedDictionary map)
            throws Exception, IOException, CircularReferenceException {
        String method = KRPCKeys.getQuery(map);
        return switch (method) {
        case KRPCKeys.Commands.PING -> {
            PingRequest pingRequest = new PingRequest();
            pingRequest.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary args = KRPCKeys.getArgs(map);
            pingRequest.from = KRPCKeys.getId(args);
            yield pingRequest;
        }
        case KRPCKeys.Commands.ANNOUNCE_PEER -> {
            AnnouncePeerRequest announcePeerRequest = new AnnouncePeerRequest();
            announcePeerRequest.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary args = KRPCKeys.getArgs(map);
            announcePeerRequest.id = KRPCKeys.getId(args);
            announcePeerRequest.infoHash = KRPCKeys.getInfoHash(args);
            announcePeerRequest.token = KRPCKeys.getToken(args);
            announcePeerRequest.implied = KRPCKeys.getImpliedPort(args);
            announcePeerRequest.port = KRPCKeys.getPort(args);
            yield announcePeerRequest;
        }
        case KRPCKeys.Commands.FIND_NODE -> {
            FindNodeRequest findNodeRequest = new FindNodeRequest();
            findNodeRequest.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary args = KRPCKeys.getArgs(map);
            findNodeRequest.id = KRPCKeys.getId(args);
            findNodeRequest.target = KRPCKeys.getTarget(args);
            yield findNodeRequest;
        }
        case KRPCKeys.Commands.GET_PEERS -> {
            GetPeersRequest getPeersRequest = new GetPeersRequest();
            getPeersRequest.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary args = KRPCKeys.getArgs(map);
            getPeersRequest.id = KRPCKeys.getId(args);
            getPeersRequest.infoHash = KRPCKeys.getInfoHash(args);
            yield getPeersRequest;
        }
        case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
            SampleInfoHashesRequest infoHashesRequest = new SampleInfoHashesRequest();
            infoHashesRequest.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary args = KRPCKeys.getArgs(map);
            infoHashesRequest.id = KRPCKeys.getId(args);
            infoHashesRequest.target = KRPCKeys.getTarget(args);
            yield infoHashesRequest;
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + method);
        };
    }

    public static Object decodeResponse(BencodedDictionary map, IRequest found)
            throws Exception, IOException, CircularReferenceException {
        return switch (found.method()) {
        case KRPCKeys.Commands.PING -> {
            PingResponse pingResponse = new PingResponse();
            BencodedDictionary rsp = KRPCKeys.getResponse(map);
            pingResponse.tid = KRPCKeys.getTransaction(map);
            pingResponse.id = KRPCKeys.getId(rsp);
            pingResponse.request = found;
            yield pingResponse;
        }
        case KRPCKeys.Commands.ANNOUNCE_PEER -> {
            AnnouncePeerResponse announcePeerResponse = new AnnouncePeerResponse();
            announcePeerResponse.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary rsp = KRPCKeys.getResponse(map);
            announcePeerResponse.id = KRPCKeys.getId(rsp);
            yield announcePeerResponse;
        }
        case KRPCKeys.Commands.FIND_NODE -> {
            FindNodeResponse findNodeResponse = new FindNodeResponse();
            findNodeResponse.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary rsp = KRPCKeys.getResponse(map);
            findNodeResponse.id = KRPCKeys.getId(rsp);
            findNodeResponse.nodes = KRPCKeys.getNodes(rsp);
            yield findNodeResponse;
        }
        case KRPCKeys.Commands.GET_PEERS -> {
            BencodedDictionary rsp = KRPCKeys.getResponse(map);
            ByteBuffer nodes = KRPCKeys.getNodes(rsp);
            var tid = KRPCKeys.getTransaction(map);
            if (nodes != null) {
                var id = KRPCKeys.getId(rsp);
                var token = KRPCKeys.getToken(rsp);
                yield new GetPeersResponse2(tid, id, token, nodes, found);
            } else {
                var id = KRPCKeys.getId(rsp);
                var token = KRPCKeys.getToken(rsp);
                BencodedList values = KRPCKeys.getValues(rsp);
                List<ByteBuffer> list = new ArrayList<>();
                for (BencodedObject bo : values) {
                    BencodedByteSequence s = (BencodedByteSequence) bo;
                    list.add(ByteBuffer.wrap(s.getByteSequence()));
                }
                yield new GetPeersResponse1(tid, id, token, list, found);
            }
        }
        case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
            SampleInfoHashesResponse infoHashesResponse = new SampleInfoHashesResponse();
            infoHashesResponse.tid = KRPCKeys.getTransaction(map);
            BencodedDictionary rsp = KRPCKeys.getResponse(map);
            infoHashesResponse.id = KRPCKeys.getId(rsp);
            infoHashesResponse.nodes = KRPCKeys.getNodes(rsp);
            infoHashesResponse.interval = KRPCKeys.getInterval(rsp);
            infoHashesResponse.num = KRPCKeys.getNum(rsp);
            infoHashesResponse.samples = KRPCKeys.getSamples(rsp);
            yield infoHashesResponse;
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + found);
        };
    }
}
