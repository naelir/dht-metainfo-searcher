package com.naelir.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.bt.messages.ext.ExtendedMessageHandshake;
import com.naelir.bt.messages.ext.UtMetadataRequest;

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
        return o.encode();
    }

    private static byte[] encode(AnnouncePeerResponse o) {
        return o.encode();
    }

    private static byte[] encode(Error o) {
        return o.encode();
    }

    private static byte[] encode(ExtendedMessageHandshake ehr) {
        return ehr.bencode();
    }

    private static byte[] encode(FindNodeRequest o) {
        return o.encode();
    }

    private static byte[] encode(FindNodeResponse o) {
        return o.encode();
    }

    private static byte[] encode(GetPeersRequest o) {
        return o.encode();
    }

    private static byte[] encode(GetPeersResponse o) {
        return o.encode();
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
        else if (o instanceof GetPeersResponse gpr)
            return encode(gpr);
        else if (o instanceof FindNodeResponse fnr)
            return encode(fnr);
        else if (o instanceof AnnouncePeerResponse apr)
            return encode(apr);
        else if (o instanceof PingResponse pr)
            return encode(pr);
        else if (o instanceof Error pr)
            return encode(pr);
        else if (o instanceof UtMetadataRequest umr)
            return encode(umr);
        else if (o instanceof ExtendedMessageHandshake eh)
            return encode(eh);
        else
            return null;
    }

    private static byte[] encode(PingRequest o) {
        return o.encode();
    }

    private static byte[] encode(PingResponse o) {
        return o.encode();
    }

    private static byte[] encode(SampleInfoHashesRequest o) {
        return o.encode();
    }
   
    private static byte[] encode(SampleInfoHashesResponse o) {
        return o.encode();
    }

    private static byte[] encode(UtMetadataRequest o) {
        return o.bencode();
    }
}
