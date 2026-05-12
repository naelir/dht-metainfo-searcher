package com.naelir.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;
import com.naeir.bt.BtKeys;
import com.naeir.bt.ExtendedHandshakeRequest;
import com.naeir.bt.UtMetaRequest;

/*Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
*/
public final class BEncoder {
    private static final Logger logger = LogManager.getLogger(BEncoder.class);

    public static byte[] bytes(BencodedDictionary query) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            query.writeObject(os);
            return os.toByteArray();
        } catch (IOException | CircularReferenceException e) {
            logger.error(e);
            return new byte[0];
        }
    }

    private static byte[] encode(AnnouncePeerRequest o) {
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
        return bytes(query);
    }

    private static byte[] encode(AnnouncePeerResponse o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        return bytes(query);
    }

    private static byte[] encode(Error o) {
        BencodedDictionary query = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        BencodedList list = new BencodedList();
        list.add(new BencodedInteger(o.status));
        list.add(new BencodedByteSequence(o.message));
        query.put(KRPCKeys.TYPE, new BencodedByteSequence(KRPCKeys.ERROR));
        query.put(KRPCKeys.ERROR, list);
        return bytes(query);
    }

    private static byte[] encode(ExtendedHandshakeRequest ehr) {
        BencodedDictionary parent = new BencodedDictionary();
        BencodedDictionary child = new BencodedDictionary();
        child.put("ut_metadata", new BencodedInteger(ehr.utCode));
        parent.put("m", child);
        return bytes(parent);
    }

    private static byte[] encode(FindNodeRequest o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(o.target.array()));
        return bytes(query);
    }

    private static byte[] encode(FindNodeResponse o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(o.nodes.array()));
        return bytes(query);
    }

    private static byte[] encode(GetPeersRequest o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.INFO_HASH, new BencodedByteSequence(o.infoHash.array()));
        return bytes(query);
    }

    private static byte[] encode(GetPeersResponse1 o) {
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
        return bytes(query);
    }

    private static byte[] encode(GetPeersResponse2 o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        args.put(new BencodedByteSequence(KRPCKeys.TOKEN), new BencodedByteSequence(o.token.array()));
        args.put(new BencodedByteSequence(KRPCKeys.NODES), new BencodedByteSequence(o.nodes.array()));
        return bytes(query);
    }

    public static byte[] encode(Object o) {
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
        else if (o instanceof SampleInfoHashesResponse sihr)
            return encode(sihr);
        else if (o instanceof GetPeersResponse1 gpr)
            return encode(gpr);
        else if (o instanceof GetPeersResponse2 gpr)
            return encode(gpr);
        else if (o instanceof FindNodeResponse fnr)
            return encode(fnr);
        else if (o instanceof AnnouncePeerResponse apr)
            return encode(apr);
        else if (o instanceof PingResponse pr)
            return encode(pr);
        else if (o instanceof Error pr)
            return encode(pr);
        else if (o instanceof ExtendedHandshakeRequest ehr)
            return encode(ehr);
        else if (o instanceof UtMetaRequest umr)
            return encode(umr);
        else
            return null;
    }

    private static byte[] encode(PingRequest o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        args.put(KRPCKeys.ID, new BencodedByteSequence(o.from.array()));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        return bytes(query);
    }

    private static byte[] encode(PingResponse o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.RESPONSE));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        args.put("id", new BencodedByteSequence(o.id.array()));
        return bytes(query);
    }

    private static byte[] encode(SampleInfoHashesRequest o) {
        BencodedDictionary query = new BencodedDictionary();
        BencodedDictionary args = new BencodedDictionary();
        query.put(new BencodedByteSequence(KRPCKeys.TRANSACTIONID), new BencodedByteSequence(o.tid.array()));
        query.put(new BencodedByteSequence(KRPCKeys.TYPE), new BencodedByteSequence(KRPCKeys.QUERY));
        query.put(new BencodedByteSequence(KRPCKeys.QUERY), new BencodedByteSequence(o.method));
        args.put(KRPCKeys.ID, new BencodedByteSequence(o.id.array()));
        args.put(KRPCKeys.TARGET, new BencodedByteSequence(o.target.array()));
        query.put(new BencodedByteSequence(KRPCKeys.ARGUMENTS), args);
        return bytes(query);
    }

    private static byte[] encode(UtMetaRequest o) {
        BencodedDictionary name = new BencodedDictionary();
        name.put(BtKeys.MSG_TYPE, new BencodedInteger(o.type));
        name.put(BtKeys.PIECE, new BencodedInteger(o.piece));
        return bytes(name);
    }
}
