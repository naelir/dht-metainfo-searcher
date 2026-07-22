package com.naelir.tracker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * UDP tracker error response (BEP-15).
 *
 * <pre>
 * Offset  Size            Name            Value
 * 0       32-bit integer  action          3 // error
 * 4       32-bit integer  transaction_id
 * 8       string          message
 * </pre>
 */
public class ErrorResponse {

    public final int transactionId;
    public final String message;

    public ErrorResponse(int transactionId, String message) {
        this.transactionId = transactionId;
        this.message = message;
    }

    public byte[] encode() {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + msgBytes.length);
        buf.putInt(TrackerAction.ERROR.code);
        buf.putInt(transactionId);
        buf.put(msgBytes);
        return buf.array();
    }

    public static ErrorResponse decode(byte[] data) {
        if (data.length < 8)
            throw new IllegalArgumentException("ErrorResponse too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int action = buf.getInt();
        if (action != TrackerAction.ERROR.code)
            throw new IllegalArgumentException("Expected action ERROR(3), got: " + action);
        int transactionId = buf.getInt();
        byte[] msgBytes = new byte[buf.remaining()];
        buf.get(msgBytes);
        String message = new String(msgBytes, StandardCharsets.UTF_8);
        return new ErrorResponse(transactionId, message);
    }

    @Override
    public String toString() {
        return "ErrorResponse[transactionId=" + transactionId + ", message=" + message + "]";
    }
}
