package com.naelir.tracker;

import java.nio.ByteBuffer;

/**
 * UDP tracker connect request (BEP-15).
 *
 * <pre>
 * Offset  Size            Name            Value
 * 0       64-bit integer  protocol_id     0x41727101980 // magic constant
 * 8       32-bit integer  action          0 // connect
 * 12      32-bit integer  transaction_id
 * 16
 * </pre>
 */
public class ConnectRequest {

    public static final long PROTOCOL_ID = 0x41727101980L;

    public final int transactionId;

    public ConnectRequest(int transactionId) {
        this.transactionId = transactionId;
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(PROTOCOL_ID);
        buf.putInt(TrackerAction.CONNECT.code);
        buf.putInt(transactionId);
        return buf.array();
    }

    public static ConnectRequest decode(byte[] data) {
        if (data.length < 16)
            throw new IllegalArgumentException("ConnectRequest too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        long protocolId = buf.getLong();
        if (protocolId != PROTOCOL_ID)
            throw new IllegalArgumentException("Invalid protocol_id: " + protocolId);
        int action = buf.getInt();
        if (action != TrackerAction.CONNECT.code)
            throw new IllegalArgumentException("Expected action CONNECT(0), got: " + action);
        int transactionId = buf.getInt();
        return new ConnectRequest(transactionId);
    }

    /** Returns true if {@code data} looks like a connect request (starts with magic). */
    public static boolean matches(byte[] data) {
        if (data == null || data.length < 16)
            return false;
        long magic = ByteBuffer.wrap(data, 0, 8).getLong();
        return magic == PROTOCOL_ID;
    }

    @Override
    public String toString() {
        return "ConnectRequest[transactionId=" + transactionId + "]";
    }
}
