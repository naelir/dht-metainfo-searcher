package com.naelir.tracker;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Generator;

/**
 * Represents an ongoing UDP tracker session for a given tracker host.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Tracks the BEP-15 connection-id and enforces the 1-minute expiry rule.</li>
 *   <li>Splits an arbitrarily large set of info-hashes into batches of at most 74
 *       (the scrape limit imposed by the spec) and works through them in order.</li>
 *   <li>Provides a {@link #reconnect()} method that resets the connection state and
 *       generates a fresh connect request with a new transaction ID, so the caller
 *       can re-key its lookup map and resume scraping from where it left off.</li>
 * </ul>
 */
public class ScrapeTrackerConnection extends TrackerConnection {
    private static final Logger logger = LogManager.getLogger(ScrapeTrackerConnection.class);

    /** Max info-hashes per scrape request (spec limit ≈ 74). */
    static final int BATCH_SIZE = 70;

    /**
     * Start index (inclusive) of the batch whose scrape response is currently
     * awaited.  -1 means no batch is in-flight.
     */
    private int pendingBatchStart = -1;

    /**
     * Index of the first hash that has NOT yet been put in a scrape request.
     * Advances by {@link #BATCH_SIZE} each time {@link #buildNextRequest()}
     * is called.
     */
    private int scrapeOffset;

    // ─────────────────────────────────────────────────────────────────────────

    public ScrapeTrackerConnection(String hostAddress, int port, Set<String> hashes) {
        super(hostAddress, port, hashes);
    }


    /**
     * Returns {@code true} when there are hashes that have not yet been put into
     * a scrape request.
     */
    public boolean hasMoreBatches() {
        return scrapeOffset < allHashes.size();
    }

    /**
     * Returns {@code true} when a scrape request has been sent but the response
     * hasn't arrived yet.
     */
    public boolean isBatchPending() {
        return pendingBatchStart >= 0;
    }

    /**
     * Returns the slice of hashes that belong to the currently in-flight scrape
     * request (the batch for which we are waiting a response).
     *
     * @throws IllegalStateException if no batch is pending
     */
    public List<String> currentBatch() {
        if (pendingBatchStart < 0)
            throw new IllegalStateException("No batch is currently pending");
        int end = Math.min(pendingBatchStart + BATCH_SIZE, allHashes.size());
        return allHashes.subList(pendingBatchStart, end);
    }

    /**
     * Marks the current in-flight batch as complete.  Call this after processing
     * a scrape response so that {@link #hasMoreBatches()} and
     * {@link #buildNextRequest()} reflect the updated state correctly.
     */
    public void completeBatch() {
        pendingBatchStart = -1;
    }

    /**
     * Builds an encoded scrape request for the next batch of up to 74 hashes,
     * advances {@link #scrapeOffset}, and records the pending batch boundaries.
     *
     * <p>The caller must ensure {@link #hasMoreBatches()} is {@code true} and
     * that the connection is not expired before calling this method.
     *
     * @return encoded scrape request bytes ready to be sent over UDP
     * @throws IllegalStateException if the connection has expired or there are
     *                               no more batches
     */
    public byte[] buildNextRequest() {
        if (isConnectionExpired())
            throw new IllegalStateException("Connection ID expired – reconnect first");
        if (!hasMoreBatches())
            throw new IllegalStateException("No more batches to scrape");

        int batchStart = scrapeOffset;
        int batchEnd   = Math.min(batchStart + BATCH_SIZE, allHashes.size());
        List<String> batch = allHashes.subList(batchStart, batchEnd);

        byte[][] hashBytes = new byte[batch.size()][];
        for (int i = 0; i < batch.size(); i++) {
            hashBytes[i] = Generator.toArray(batch.get(i));
        }

        pendingBatchStart = batchStart;
        scrapeOffset      = batchEnd;

        logger.debug("Scraping batch [{},{}) of {} hashes against {}:{}",
                batchStart, batchEnd, allHashes.size(), hostAddress, port);

        return new ScrapeRequest(connectionId, transactionId, hashBytes).encode();
    }


    /**
     * Resets the connection state and assigns a fresh transaction ID so the
     * caller can re-key its map and start a new connect handshake.
     *
     * <p>The scrape offset is preserved – scraping resumes from where it stopped.
     * If a batch was in-flight when the connection expired, that batch is
     * rewound so it will be retried after reconnection.
     */
    public void reconnect() {
        logger.debug("Reconnecting to {}:{} (scrapeOffset={}, pendingBatchStart={})",
                hostAddress, port, scrapeOffset, pendingBatchStart);

        // Rewind the in-flight batch so it gets retried after reconnection.
        if (pendingBatchStart >= 0) {
            scrapeOffset      = pendingBatchStart;
            pendingBatchStart = -1;
        }

        super.reconnect();
    }
    
    @Override
    public String toString() {
        return "ScrapeTrackerConnection{host=" + hostAddress + ":" + port
                + ", tid=" + transactionId
                + ", offset=" + scrapeOffset + "/" + allHashes.size()
                + ", expired=" + isConnectionExpired() + "}";
    }
}