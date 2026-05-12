package com.naelir.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;

/*Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
*/
public class BEncoder {
    public static byte[] encode(AnnouncePeerRequest o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.INFO_HASH, new BencodedByteSequence(o.infoHash.array()));
        args.put(KRPCKeys.PORT, new BencodedInteger(o.port));
        args.put(KRPCKeys.TOKEN, new BencodedByteSequence(o.token.toString()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(AnnouncePeerResponse o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(Error o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        BencodedList list = new BencodedList();
        list.add(new BencodedInteger(o.status));
        list.add(new BencodedByteSequence(o.message));
        query.put(KRPCKeys.TYPE, new BencodedByteSequence(KRPCKeys.ERROR));
        query.put(KRPCKeys.ERROR, list);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(FindNodeRequest o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(o.target.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(FindNodeResponse o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(o.nodes.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(GetPeersRequest o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.INFO_HASH, new BencodedByteSequence(o.infoHash.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(GetPeersResponse1 o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.TOKEN), new BencodedByteSequence(o.token.array()));
        BencodedList name = new BencodedList();
        for (ByteBuffer v : o.values) {
            name.add(new BencodedByteSequence(v.array()));
        }
        args.put(new BencodedByteSequence(KRPCKeys.VALUES), name);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(GetPeersResponse2 o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.TOKEN), new BencodedByteSequence(o.token.array()));
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(o.nodes.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(Object o) throws IOException, CircularReferenceException {
        if (o instanceof SampleInfoHashesRequest sihr)
            return encode(sihr);
        else if (o instanceof GetPeersRequest gpr)
            return encode(gpr);
        else if (o instanceof FindNodeRequest fnr)
            return encode(fnr);
        else if (o instanceof AnnouncePeerRequest apr)
            return encode(apr);
        else if (o instanceof PingRequest pr)
            return encode(pr);
        else
            return null;
    }

    private static byte[] encode(PingRequest o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        args.put(KRPCKeys.ID, new BencodedByteSequence(o.from.array()));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    public static byte[] encode(PingResponse o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }

    private static byte[] encode(SampleInfoHashesRequest o) throws IOException, CircularReferenceException {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        args.put(KRPCKeys.ID, new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(o.target.array()));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        query.writeObject(os);
        return os.toByteArray();
    }
}
