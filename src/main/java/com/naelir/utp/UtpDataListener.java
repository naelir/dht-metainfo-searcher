package com.naelir.utp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import com.naelir.dht.IOnDataListener;

public class UtpDataListener implements IOnDataListener {

    private UTPManager utpManager;

    public UtpDataListener(UTPManager utpManager) {
        this.utpManager = utpManager;
    }

    public UTPManager getUtpManager() {
        return utpManager;
    }

    @Override
    public Optional<byte[]> onData(byte[] data, InetAddress addr, int port) {
        byte[] response = utpManager.handlePacket(data, new InetSocketAddress(addr, port));
        return response != null ? Optional.of(response) : Optional.empty();
    }

}
