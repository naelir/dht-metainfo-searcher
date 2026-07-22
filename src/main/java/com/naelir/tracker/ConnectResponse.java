package com.naelir.tracker;

import java.nio.ByteBuffer;

/**
 * UDP tracker connect response (BEP-15).
 *
 * <pre>
 * Offset  Size            Name            Value
 * 0       32-bit integer  action          0 // connect
 * 4       32-bit integer  transaction_id
 * 8       64-bit integer  connection_id
 * 16
 * </pre>
 */
public class ConnectResponse {

    public final int transactionId;
    public final long connectionId;

    public ConnectResponse(int transactionId, long connectionId) {
        this.transactionId = transactionId;
        this.connectionId = connectionId;
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(TrackerAction.CONNECT.code);
        buf.putInt(transactionId);
        buf.putLong(connectionId);
        return buf.array();
    }

    public static ConnectResponse decode(byte[] data) {
        if (data.length < 16)
            throw new IllegalArgumentException("ConnectResponse too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int action = buf.getInt();
        if (action != TrackerAction.CONNECT.code)
            throw new IllegalArgumentException("Expected action CONNECT(0), got: " + action);
        int transactionId = buf.getInt();
        long connectionId = buf.getLong();
        return new ConnectResponse(transactionId, connectionId);
    }

    @Override
    public String toString() {
        return "ConnectResponse[transactionId=" + transactionId + ", connectionId=" + connectionId + "]";
    }
}
