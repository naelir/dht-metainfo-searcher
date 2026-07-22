package com.naelir.tracker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UDP tracker scrape response (BEP-15).
 *
 * <pre>
 * Offset      Size            Name            Value
 * 0           32-bit integer  action          2 // scrape
 * 4           32-bit integer  transaction_id
 * 8 + 12 * n  32-bit integer  seeders
 * 12 + 12 * n 32-bit integer  completed
 * 16 + 12 * n 32-bit integer  leechers
 * 8 + 12 * N
 * </pre>
 */
public class ScrapeResponse {

    public record TorrentStats(int seeders, int completed, int leechers) {
        @Override
        public String toString() {
            return "{seeders=" + seeders + ", completed=" + completed + ", leechers=" + leechers + "}";
        }
    }

    public final int transactionId;
    public final List<TorrentStats> stats;

    public ScrapeResponse(int transactionId, List<TorrentStats> stats) {
        this.transactionId = transactionId;
        this.stats = Collections.unmodifiableList(stats);
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(8 + 12 * stats.size());
        buf.putInt(TrackerAction.SCRAPE.code);
        buf.putInt(transactionId);
        for (TorrentStats s : stats) {
            buf.putInt(s.seeders());
            buf.putInt(s.completed());
            buf.putInt(s.leechers());
        }
        return buf.array();
    }

    public static ScrapeResponse decode(byte[] data) {
        if (data.length < 8)
            throw new IllegalArgumentException("ScrapeResponse too short: " + data.length);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int action = buf.getInt();
        if (action != TrackerAction.SCRAPE.code)
            throw new IllegalArgumentException("Expected action SCRAPE(2), got: " + action);
        int transactionId = buf.getInt();
        List<TorrentStats> stats = new ArrayList<>();
        while (buf.remaining() >= 12) {
            int seeders = buf.getInt();
            int completed = buf.getInt();
            int leechers = buf.getInt();
            stats.add(new TorrentStats(seeders, completed, leechers));
        }
        return new ScrapeResponse(transactionId, stats);
    }

    @Override
    public String toString() {
        return "ScrapeResponse[transactionId=" + transactionId + ", torrents=" + stats.size() + "]";
    }
}
