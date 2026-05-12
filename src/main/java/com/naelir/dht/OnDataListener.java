package com.naelir.dht;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class OnDataListener implements IOnDataListener {
    public static final Logger logger = LogManager.getLogger(OnDataListener.class);
    private ByteBuffer myself;
    private ResponseResolver resolver;

    public OnDataListener(Data data) {
        this.myself = Generator.generateRandomID();
        this.resolver = new ResponseResolver(data);
    }

    @Override
    public Optional<byte[]> onData(byte[] data, InetAddress address, int port) {
        From from = new From(address.getAddress(), port);
        Optional<BencodedDictionary> decode = BDecoder.decode(data);
        if (decode.isPresent()) {
            BencodedDictionary bmap = decode.get();
            return this.resolver.resolve(bmap, from);
        } else
            return Optional.empty();
    }
}
