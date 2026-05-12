package com.naelir.dht;

import java.net.InetAddress;
import java.util.Optional;

public interface IOnDataListener {
    Optional<byte[]> onData(byte[] data, InetAddress addr, int port);
}
