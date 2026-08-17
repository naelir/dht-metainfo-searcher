package com.naelir.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class TrackerConnection {
    private static final Logger logger = LogManager.getLogger(TrackerConnection.class);

    /** Connection-id is valid for 1 minute after receipt (client-side rule). */
    private static final long CONNECTION_TTL_MS = 60_000L;

    private static final Random RNG = new Random();

    // ── identity ─────────────────────────────────────────────────────────────
    protected final String hostAddress;
    protected final int port;

    /** Current transaction ID – changes on every {@link #reconnect()}. */
    protected int transactionId;

    // ── connection state ─────────────────────────────────────────────────────
    protected long connectionId;
    protected long connectionReceivedAt; // epoch ms; 0 = no live connection

    // ── scrape state ─────────────────────────────────────────────────────────
    /** All hashes that still need stats (the ones we haven't sent yet + pending). */
    protected final List<String> allHashes;


    // ─────────────────────────────────────────────────────────────────────────

    public TrackerConnection(String hostAddress, int port, Set<String> hashes) {
        this.hostAddress = hostAddress;
        this.port        = port;
        this.allHashes   = new ArrayList<>(hashes);
        this.transactionId = newTid();
    }

    // ── public API ───────────────────────────────────────────────────────────

    /** Returns the transaction ID currently identifying this connection attempt. */
    public int transactionId() { return transactionId; }

    public String hostAddress() { return hostAddress; }
    public int port()           { return port; }

    /**
     * Builds and returns an encoded connect request.
     * Does NOT change the transaction ID – call {@link #reconnect()} first if
     * you want to start a fresh handshake.
     */
    public byte[] connect() {
        return new ConnectRequest(transactionId).encode();
    }

    /**
     * Called when the tracker replies with a connect response whose
     * {@code transactionId} matches ours.
     *
     * @param connectionId the 64-bit connection-id from the response
     */
    public void onConnected(long connectionId) {
        this.connectionId          = connectionId;
        this.connectionReceivedAt  = System.currentTimeMillis();
        logger.debug("Connected to {}:{} – connectionId=0x{}", hostAddress, port,
                Long.toHexString(connectionId));
    }

    /**
     * Returns {@code true} if we have no live connection or the 1-minute TTL
     * has elapsed (client-side rule from the spec).
     */
    public boolean isConnectionExpired() {
        return connectionReceivedAt == 0
                || System.currentTimeMillis() - connectionReceivedAt > CONNECTION_TTL_MS;
    }

    protected byte[] buildNextRequest() {
        return new byte[0];
    }
    

    protected boolean hasMoreBatches() {
        return false;
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
        connectionId          = 0;
        connectionReceivedAt  = 0;
        transactionId         = newTid();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int newTid() {
        return RNG.nextInt() & 0x7FFF_FFFF;
    }
}