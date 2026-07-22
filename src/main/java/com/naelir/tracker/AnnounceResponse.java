package com.naelir.tracker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UDP tracker IPv4 announce response (BEP-15).
 *
 * <pre>
 * Offset      Size            Name            Value
 * 0           32-bit integer  action          1 // announce
 * 4           32-bit integer  transaction_id
 * 8           32-bit integer  interval
 * 12          32-bit integer  leechers
 * 16          32-bit integer  seeders
 * 20 + 6 * n  32-bit integer  IP address
 * 24 + 6 * n  16-bit integer  TCP port
 * 20 + 6 * N
 * </pre>
 */
public class AnnounceResponse {

    public record Peer(InetAddress address, int port) {
        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }
    }

    public final int transactionId;
    public final int interval;
    public final int leechers;
    public final int seeders;
    public final List<Peer> peers;

    public AnnounceResponse(int transactionId, int interval, int leechers, int seeders, List<Peer> peers) {
        this.transactionId = transactionId;
        this.interval = interval;
        this.leechers = leechers;
        this.seeders = seeders;
        this.peers = Collections.unmodifiableList(peers);
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(20 + 6 * peers.size());
        buf.putInt(TrackerAction.ANNOUNCE.code);
        buf.putInt(transactionId);
        buf.putInt(interval);
        buf.putInt(leechers);
        buf.putInt(seeders);
        for (Peer peer : peers) {
            buf.put(peer.address().getAddress());
            buf.putShort((short) peer.port());
        }
        return buf.array();
    }

    public static AnnounceResponse decode(byte[] data) {
        if (data.length < 20)
            throw new IllegalArgumentException("AnnounceResponse too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int action = buf.getInt();
        if (action != TrackerAction.ANNOUNCE.code)
            throw new IllegalArgumentException("Expected action ANNOUNCE(1), got: " + action);
        int transactionId = buf.getInt();
        int interval = buf.getInt();
        int leechers = buf.getInt();
        int seeders = buf.getInt();
        List<Peer> peers = new ArrayList<>();
        while (buf.remaining() >= 6) {
            byte[] ip = new byte[4];
            buf.get(ip);
            int port = Short.toUnsignedInt(buf.getShort());
            try {
                peers.add(new Peer(InetAddress.getByAddress(ip), port));
            } catch (UnknownHostException e) {
                // 4-byte array always valid, skip
            }
        }
        return new AnnounceResponse(transactionId, interval, leechers, seeders, peers);
    }

    @Override
    public String toString() {
        return "AnnounceResponse[transactionId=" + transactionId + ", interval=" + interval
                + ", leechers=" + leechers + ", seeders=" + seeders + ", peers=" + peers.size() + "]";
    }
}
