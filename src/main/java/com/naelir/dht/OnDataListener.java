package com.naelir.dht;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class OnDataListener implements IOnDataListener {
    public static final Logger logger = LogManager.getLogger(OnDataListener.class);
    private ResponseResolver resolver;

    public OnDataListener(Data data) {
        this.resolver = new ResponseResolver(data);
    }

    @Override
    public Optional<byte[]> onData(byte[] buffer, InetAddress address, int port) {
        From from = new From(address.getAddress(), port);
        Optional<BencodedDictionary> decode = BDecoder.decode(buffer);
        if (decode.isPresent()) {
            BencodedDictionary bmap = decode.get();
            if (bmap.get("error") != null) {
                logger.error("cannot decode from {}, port {} {}", Arrays.toString(buffer), address, port);
            } else
                return this.resolver.resolve(bmap, from);
        }
        return Optional.empty();
    }
}
