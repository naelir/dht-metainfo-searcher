package com.naelir.utp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UtpDataListener {
    public static final Logger logger = LogManager.getLogger(UtpDataListener.class);
    private UTPManager utpManager;

    public UtpDataListener(UTPManager utpManager) {
        this.utpManager = utpManager;
    }

    public UTPManager getUtpManager() {
        return this.utpManager;
    }

    public Optional<byte[]> onData(byte[] data, InetAddress addr, int port) {
        logger.debug("Received data from {}:{}", addr.getHostAddress(), port);
        byte[] response = this.utpManager.handlePacket(data, new InetSocketAddress(addr, port));
        return response != null ? Optional.of(response) : Optional.empty();
    }
}
