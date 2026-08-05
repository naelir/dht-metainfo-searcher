package com.naelir.tracker;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;

/**
 * Network-layer relay for incoming UDP tracker datagrams (BEP-15).
 *
 * <p>Mirrors the design of {@code UtpOnDataListener}: this class is kept thin
 * and delegates all protocol logic to {@link TrackerUdpManager#handlePacket}.
 */
public class TrackerOnDataListener {
    public static final Logger logger = LogManager.getLogger(TrackerOnDataListener.class);
    private final TrackerUdpManager trackerUdpManager;

    public TrackerOnDataListener(Data data) {
        this.trackerUdpManager = new TrackerUdpManager(data);
    }

    public TrackerUdpManager getTrackerUdpManager() {
        return trackerUdpManager;
    }

    /**
     * Called by the network layer for every incoming UDP datagram identified
     * as a tracker protocol packet.
     *
     * @param buffer  raw datagram bytes
     * @param address sender's IP address
     * @param port    sender's UDP port
     * @return an optional response to send back to the sender
     */
    public Optional<byte[]> onData(byte[] buffer, InetAddress address, int port) {
        logger.debug("Received tracker data from {}:{}", address.getHostAddress(), port);
        byte[] response = trackerUdpManager.handlePacket(buffer, new InetSocketAddress(address, port));
        return response != null ? Optional.of(response) : Optional.empty();
    }

    public static boolean isTrackerPacket(byte[] data) {
        return TrackerUdpManager.isTrackerPacket(data);
    }
}