package com.naelir.dht;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.BencodeFormatException;
import com.github.cdefgah.bencoder4j.io.BencodeStreamIterator;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;

public final class BDecoder {
    public static final Logger logger = LogManager.getLogger(BDecoder.class);

    public static Optional<BencodedDictionary> decode(byte[] data) {
        try {
            return Optional.of(map(data));
        } catch (IOException | BencodeFormatException e) {
            return Optional.of(error());
        }
    }

    static BencodedDictionary error() {
        BencodedDictionary bencodedDictionary = new BencodedDictionary();
        bencodedDictionary.put("error", new BencodedInteger(1));
        return bencodedDictionary;
    }

    public static boolean isBencode(byte[] buffer) {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        BencodeStreamIterator bsr = new BencodeStreamIterator(bais);
        try {
            return bsr.hasNext();
        } catch (IOException e) {
            logger.error("cannot isBencode {}", Arrays.toString(buffer), e);
            return false;
        }
    }

    static BencodedDictionary map(byte[] buffer) throws IOException, BencodeFormatException {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        BencodeStreamIterator bsr = new BencodeStreamIterator(bais);
        return (BencodedDictionary) bsr.next();
    }
    
    public static Optional<Frame> decodeSized(byte[] buffer) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            BencodeStreamIterator bsr = new BencodeStreamIterator(bais);
            BencodedDictionary next = (BencodedDictionary) bsr.next();
            return Optional.of(new Frame(next, buffer.length - bais.available()));
        } catch (IOException | BencodeFormatException e) {
            return Optional.empty();
        }
    }
    
    
    public static class Frame {
        public final BencodedDictionary map;
        public final int size;
        public Frame(BencodedDictionary map, int size) {
            this.map = map;
            this.size = size;
        }
        
    }
}
