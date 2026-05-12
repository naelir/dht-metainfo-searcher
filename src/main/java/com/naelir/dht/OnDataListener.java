package com.naelir.dht;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.utp.UTPManager;

public class OnDataListener implements IOnDataListener {
    public static final Logger logger = LogManager.getLogger(OnDataListener.class);
    private ResponseResolver resolver;
    private UTPManager manager;

    public OnDataListener(Data data) {
        this.resolver = new ResponseResolver(data);
        this.manager = new UTPManager();
    }

    @Override
    public Optional<byte[]> onData(byte[] buffer, InetAddress address, int port) {
        From from = new From(address.getAddress(), port);
        Optional<BencodedDictionary> decode = BDecoder.decode(buffer);
        if (decode.isPresent()) {
            BencodedDictionary bmap = decode.get();
            if (bmap.get("error") != null) {
                logger.error("cannot decode from address {}, port {}",address, port);
            } else
                return this.resolver.resolve(bmap, from);
        } else {
            byte[] handlePacket = this.manager.handlePacket(buffer, new InetSocketAddress(address, port));
            if (handlePacket != null) {
                return Optional.of(handlePacket);
            }
        }
        return Optional.empty();
    }
}
