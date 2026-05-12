package com.naelir.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedList;
import com.github.cdefgah.bencoder4j.model.BencodedObject;

public class CommandDecoder {
    public static Object decode(BencodedDictionary map, String method)
            throws Exception, IOException, CircularReferenceException {
        String type = KRPCKeys.getType(map);
//        String method = KRPCKeys.getQuery(map);
        if (KRPCKeys.QUERY.equals(type))
            return switch (method) {
            case KRPCKeys.Commands.PING -> {
                PingRequest pingRequest = new PingRequest();
                pingRequest.tid = KRPCKeys.getTransaction(map);
                pingRequest.from = KRPCKeys.getId(map);
                yield pingRequest;
            }
            case KRPCKeys.Commands.ANNOUNCE_PEER -> {
                AnnouncePeerRequest announcePeerRequest = new AnnouncePeerRequest();
                announcePeerRequest.tid = KRPCKeys.getTransaction(map);
                announcePeerRequest.id = KRPCKeys.getId(map);
                announcePeerRequest.infoHash = KRPCKeys.getInfoHash(map);
                announcePeerRequest.token = KRPCKeys.getToken(map);
                announcePeerRequest.implied = KRPCKeys.getImpliedPort(map);
                announcePeerRequest.port = KRPCKeys.getPort(map);
                yield announcePeerRequest;
            }
            case KRPCKeys.Commands.FIND_NODE -> {
                FindNodeRequest findNodeRequest = new FindNodeRequest();
                findNodeRequest.tid = KRPCKeys.getTransaction(map);
                findNodeRequest.id = KRPCKeys.getId(map);
                findNodeRequest.target = KRPCKeys.getTarget(map);
                yield findNodeRequest;
            }
            case KRPCKeys.Commands.GET_PEERS -> {
                GetPeersRequest getPeersRequest = new GetPeersRequest();
                getPeersRequest.tid = KRPCKeys.getTransaction(map);
                getPeersRequest.id = KRPCKeys.getId(map);
                getPeersRequest.infoHash = KRPCKeys.getInfoHash(map);
                yield getPeersRequest;
            }
            case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
                SampleInfoHashesRequest infoHashesRequest = new SampleInfoHashesRequest();
                infoHashesRequest.tid = KRPCKeys.getTransaction(map);
                infoHashesRequest.id = KRPCKeys.getId(map);
                infoHashesRequest.target = KRPCKeys.getTarget(map);
                yield infoHashesRequest;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + method);
            };
        else if (KRPCKeys.RESPONSE.equals(type))
            return switch (method) {
            case KRPCKeys.Commands.PING -> {
                PingResponse pingResponse = new PingResponse();
                pingResponse.tid = KRPCKeys.getTransaction(map);
                pingResponse.id = KRPCKeys.getId(map);
                yield pingResponse;
            }
            case KRPCKeys.Commands.ANNOUNCE_PEER -> {
                AnnouncePeerResponse announcePeerResponse = new AnnouncePeerResponse();
                announcePeerResponse.tid = KRPCKeys.getTransaction(map);
                announcePeerResponse.id = KRPCKeys.getId(map);
                yield announcePeerResponse;
            }
            case KRPCKeys.Commands.FIND_NODE -> {
                FindNodeResponse findNodeResponse = new FindNodeResponse();
                findNodeResponse.tid = KRPCKeys.getTransaction(map);
                findNodeResponse.id = KRPCKeys.getId(map);
                findNodeResponse.nodes = KRPCKeys.getNodes(map);
                yield findNodeResponse;
            }
            case KRPCKeys.Commands.GET_PEERS -> {
                ByteBuffer nodes = KRPCKeys.getNodes(map);
                if (nodes != null) {
                    var tid = KRPCKeys.getTransaction(map);
                    var id = KRPCKeys.getId(map);
                    var token = KRPCKeys.getToken(map);
                    yield new GetPeersResponse2(tid, id, token, nodes);
                } else {
                    var tid = KRPCKeys.getTransaction(map);
                    var id = KRPCKeys.getId(map);
                    var token = KRPCKeys.getToken(map);
                    BencodedList values = KRPCKeys.getValues(map);
                    List<ByteBuffer> list = new ArrayList<>();
                    for (BencodedObject bo : values) {
                        BencodedByteSequence s = (BencodedByteSequence) bo;
                        list.add(ByteBuffer.wrap(s.getByteSequence()));
                    }
                    yield new GetPeersResponse1(tid, id, token, list);
                }
            }
            case KRPCKeys.Commands.SAMPLE_INFOHASHES -> {
                SampleInfoHashesResponse infoHashesResponse = new SampleInfoHashesResponse();
                infoHashesResponse.tid = KRPCKeys.getTransaction(map);
                infoHashesResponse.id = KRPCKeys.getId(map);
                infoHashesResponse.nodes = KRPCKeys.getNodes(map);
                infoHashesResponse.interval = KRPCKeys.getInterval(map);
                infoHashesResponse.num = KRPCKeys.getNum(map);
                infoHashesResponse.samples = KRPCKeys.getSamples(map);
                yield infoHashesResponse;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + method);
            };
        else if (KRPCKeys.ERROR.equals(type)) {
            Error error = new Error();
            error.tid = KRPCKeys.getTransaction(map);
            BencodedList list = KRPCKeys.getError(map);
            BencodedByteSequence code = (BencodedByteSequence) list.get(0);
            BencodedByteSequence message = (BencodedByteSequence) list.get(1);
            error.status = Integer.parseInt(code.toUTF8String());
            error.message = message.toUTF8String();
            return error;
        } else
            throw new IllegalArgumentException("Unexpected value: " + type);
    }
}
