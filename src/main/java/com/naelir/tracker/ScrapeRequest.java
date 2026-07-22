package com.naelir.tracker;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * UDP tracker scrape request (BEP-15).
 * Up to ~74 info-hashes can be scraped at once.
 *
 * <pre>
 * Offset          Size            Name            Value
 * 0               64-bit integer  connection_id
 * 8               32-bit integer  action          2 // scrape
 * 12              32-bit integer  transaction_id
 * 16 + 20 * n     20-byte string  info_hash
 * 16 + 20 * N
 * </pre>
 */
public class ScrapeRequest {

    public final long connectionId;
    public final int transactionId;
    /** Each element is exactly 20 bytes. */
    public final byte[][] infoHashes;

    public ScrapeRequest(long connectionId, int transactionId, byte[]... infoHashes) {
        this.connectionId = connectionId;
        this.transactionId = transactionId;
        this.infoHashes = Arrays.copyOf(infoHashes, infoHashes.length);
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(16 + 20 * infoHashes.length);
        buf.putLong(connectionId);
        buf.putInt(TrackerAction.SCRAPE.code);
        buf.putInt(transactionId);
        for (byte[] hash : infoHashes) {
            buf.put(hash, 0, Math.min(hash.length, 20));
        }
        return buf.array();
    }

    public static ScrapeRequest decode(byte[] data) {
        if (data.length < 16)
            throw new IllegalArgumentException("ScrapeRequest too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        long connectionId = buf.getLong();
        int action = buf.getInt();
        if (action != TrackerAction.SCRAPE.code)
            throw new IllegalArgumentException("Expected action SCRAPE(2), got: " + action);
        int transactionId = buf.getInt();
        int hashCount = buf.remaining() / 20;
        byte[][] hashes = new byte[hashCount][20];
        for (int i = 0; i < hashCount; i++) {
            buf.get(hashes[i]);
        }
        return new ScrapeRequest(connectionId, transactionId, hashes);
    }

    @Override
    public String toString() {
        return "ScrapeRequest[transactionId=" + transactionId + ", infoHashes=" + infoHashes.length + "]";
    }
}
