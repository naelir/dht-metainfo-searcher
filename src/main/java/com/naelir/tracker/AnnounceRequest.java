package com.naelir.tracker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * UDP tracker IPv4 announce request (BEP-15).
 *
 * <pre>
 * Offset  Size            Name            Value
 * 0       64-bit integer  connection_id
 * 8       32-bit integer  action          1 // announce
 * 12      32-bit integer  transaction_id
 * 16      20-byte string  info_hash
 * 36      20-byte string  peer_id
 * 56      64-bit integer  downloaded
 * 64      64-bit integer  left
 * 72      64-bit integer  uploaded
 * 80      32-bit integer  event           0=none; 1=completed; 2=started; 3=stopped
 * 84      32-bit integer  IP address      0 // default
 * 88      32-bit integer  key
 * 92      32-bit integer  num_want        -1 // default
 * 96      16-bit integer  port
 * 98
 * </pre>
 */
public class AnnounceRequest {

    public enum Event {
        NONE(0), COMPLETED(1), STARTED(2), STOPPED(3);

        public final int code;

        Event(int code) {
            this.code = code;
        }

        public static Event of(int code) {
            return switch (code) {
                case 0 -> NONE;
                case 1 -> COMPLETED;
                case 2 -> STARTED;
                case 3 -> STOPPED;
                default -> throw new IllegalArgumentException("Unknown event: " + code);
            };
        }
    }

    public final long connectionId;
    public final int transactionId;
    /** 20-byte info hash. */
    public final byte[] infoHash;
    /** 20-byte peer id. */
    public final byte[] peerId;
    public final long downloaded;
    public final long left;
    public final long uploaded;
    public final Event event;
    /** IPv4 address (4 bytes), 0 = default. */
    public final int ipAddress;
    public final int key;
    /** Number of peers wanted, -1 = default. */
    public final int numWant;
    public final short port;

    public AnnounceRequest(long connectionId, int transactionId, byte[] infoHash, byte[] peerId,
            long downloaded, long left, long uploaded, Event event,
            int ipAddress, int key, int numWant, short port) {
        this.connectionId = connectionId;
        this.transactionId = transactionId;
        this.infoHash = Arrays.copyOf(infoHash, 20);
        this.peerId = Arrays.copyOf(peerId, 20);
        this.downloaded = downloaded;
        this.left = left;
        this.uploaded = uploaded;
        this.event = event;
        this.ipAddress = ipAddress;
        this.key = key;
        this.numWant = numWant;
        this.port = port;
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(98);
        buf.putLong(connectionId);
        buf.putInt(TrackerAction.ANNOUNCE.code);
        buf.putInt(transactionId);
        buf.put(infoHash);
        buf.put(peerId);
        buf.putLong(downloaded);
        buf.putLong(left);
        buf.putLong(uploaded);
        buf.putInt(event.code);
        buf.putInt(ipAddress);
        buf.putInt(key);
        buf.putInt(numWant);
        buf.putShort(port);
        return buf.array();
    }

    public static AnnounceRequest decode(byte[] data) {
        if (data.length < 98)
            throw new IllegalArgumentException("AnnounceRequest too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        long connectionId = buf.getLong();
        int action = buf.getInt();
        if (action != TrackerAction.ANNOUNCE.code)
            throw new IllegalArgumentException("Expected action ANNOUNCE(1), got: " + action);
        int transactionId = buf.getInt();
        byte[] infoHash = new byte[20];
        buf.get(infoHash);
        byte[] peerId = new byte[20];
        buf.get(peerId);
        long downloaded = buf.getLong();
        long left = buf.getLong();
        long uploaded = buf.getLong();
        Event event = Event.of(buf.getInt());
        int ipAddress = buf.getInt();
        int key = buf.getInt();
        int numWant = buf.getInt();
        short port = buf.getShort();
        return new AnnounceRequest(connectionId, transactionId, infoHash, peerId,
                downloaded, left, uploaded, event, ipAddress, key, numWant, port);
    }

    /** IPv4 peers included in the response. */
    public record Peer(InetAddress address, int port) {
        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }
    }

    @Override
    public String toString() {
        return "AnnounceRequest[transactionId=" + transactionId + ", event=" + event + ", port=" + port + "]";
    }
}
