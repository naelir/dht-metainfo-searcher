package com.naelir.dht;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.BencodeFormatException;
import com.github.cdefgah.bencoder4j.io.BencodeStreamIterator;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class BDecoder {
    public static final Logger logger = LogManager.getLogger(BDecoder.class);

    static Optional<BencodedDictionary> decode(byte[] data) {
        try {
            return Optional.of(map(data));
        } catch (IOException | BencodeFormatException e) {
            logger.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    static BencodedDictionary map(byte[] buffer) throws IOException, BencodeFormatException {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        BencodeStreamIterator bsr = new BencodeStreamIterator(bais);
        return (BencodedDictionary) bsr.next();
    }
}
