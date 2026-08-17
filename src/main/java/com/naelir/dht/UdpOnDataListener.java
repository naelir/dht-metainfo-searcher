package com.naelir.dht;

import java.net.InetAddress;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class UdpOnDataListener {
    public static final Logger logger = LogManager.getLogger(UdpOnDataListener.class);
    private DhtResponseResolver resolver;

    public UdpOnDataListener(DhtResponseResolver resolver) {
        this.resolver = resolver;
    }

    public Optional<byte[]> onData(byte[] buffer, InetAddress address, int port) {
        From from = new From(address.getAddress(), port);
        Optional<BencodedDictionary> decode = BDecoder.decode(buffer);
        if (decode.isPresent()) {
            BencodedDictionary bmap = decode.get();
            if (bmap.get("error") != null) {
                logger.debug("cannot decode from address {}, port {}", address, port);
            } else
                return this.resolver.resolve(bmap, from);
        }
        return Optional.empty();
    }
}
