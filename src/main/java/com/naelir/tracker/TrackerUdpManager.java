package com.naelir.tracker;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.From;
import com.naelir.tracker.ScrapeResponse.TorrentStats;

/**
 * Core logic for the BitTorrent UDP tracker protocol (BEP-15).
 *
 * <p>Manages a set of {@link TrackerConnection} sessions, each of which may
 * cover an arbitrarily large number of info-hashes split into batches of at
 * most 74 (the spec limit).  When the 1-minute connection-id TTL expires
 * mid-session the manager automatically issues a new connect handshake and
 * resumes scraping from the last completed batch.
 *
 * <p>{@link TrackerOnDataListener} acts as a thin relay that forwards every
 * incoming datagram here via {@link #handlePacket}.
 */
public class TrackerUdpManager {
    public static final Logger logger = LogManager.getLogger(TrackerUdpManager.class);

    private final Data data;

    /**
     * Live sessions keyed by the <em>current</em> transaction ID of each
     * {@link TrackerConnection}.  The key is updated whenever
     * {@link TrackerConnection#reconnect()} is called.
     */
    private final Map<Integer, TrackerConnection> connections = new HashMap<>();

    public TrackerUdpManager(Data data) {
        this.data = data;
    }

    // ── static helper ────────────────────────────────────────────────────────

    public static boolean isTrackerPacket(byte[] data) {
        if (data == null || data.length < 4)
            return false;
        if (data.length >= 16 && ConnectRequest.matches(data))
            return true;
        int action = ByteBuffer.wrap(data, 0, 4).getInt();
        return action >= 0 && action <= 3 && data[0] == 0x00;
    }

    // ── entry point ──────────────────────────────────────────────────────────

    /**
     * Parses a raw datagram and returns an optional response to send back.
     *
     * @param buffer raw datagram bytes
     * @param sender sender's socket address
     * @return response bytes, or {@code null} when no reply is needed
     */
    public byte[] handlePacket(byte[] buffer, InetSocketAddress sender) {
        From from = new From(sender.getAddress().getAddress(), sender.getPort());
        try {
            if (ConnectRequest.matches(buffer))
                return onConnectRequest(ConnectRequest.decode(buffer), from).orElse(null);

            if (buffer.length < 4) {
                logger.warn("Tracker packet from {} too short ({} bytes)", from, buffer.length);
                return null;
            }

            int action = ByteBuffer.wrap(buffer, 0, 4).getInt();
            Optional<byte[]> result = switch (action) {
                case 0 -> onConnectResponse(ConnectResponse.decode(buffer), from);
                case 1 -> onAnnounceResponse(AnnounceResponse.decode(buffer), from);
                case 2 -> onScrapeResponse(ScrapeResponse.decode(buffer), from);
                case 3 -> onErrorResponse(ErrorResponse.decode(buffer), from);
                default -> {
                    logger.warn("Unknown tracker action {} from {}", action, from);
                    yield Optional.empty();
                }
            };
            return result.orElse(null);
        } catch (Exception e) {
            logger.error("Failed to parse tracker packet from {}: {}", from, e.getMessage(), e);
            return null;
        }
    }

    // ── session factory ──────────────────────────────────────────────────────

    /**
     * Creates a new {@link TrackerConnection} for the given tracker and registers
     * it.  Returns the encoded connect-request bytes that must be sent to the
     * tracker to start the handshake.
     *
     * @param hashes      info-hashes to scrape (any size; batched automatically)
     * @param hostAddress tracker host
     * @param port        tracker UDP port
     * @return encoded {@link ConnectRequest} bytes ready for sending
     */
    public byte[] newConnection(Set<String> hashes, String hostAddress, int port) {
        TrackerConnection tc = new TrackerConnection(hostAddress, port, hashes);
        connections.put(tc.transactionId(), tc);
        logger.info("Initiating tracker session {}:{} for {} hashes in {} batch(es)",
                hostAddress, port, hashes.size(),
                (int) Math.ceil((double) hashes.size() / TrackerConnection.BATCH_SIZE));
        return tc.connect();
    }

    // ── handlers ─────────────────────────────────────────────────────────────

    protected Optional<byte[]> onConnectRequest(ConnectRequest req, From from) {
        logger.debug("Tracker ← {} from {}", req, from);
        return Optional.empty();
    }

    /**
     * A tracker has replied to our connect request.
     * Store the connection-id and fire the first scrape batch.
     */
    protected Optional<byte[]> onConnectResponse(ConnectResponse resp, From from) {
        TrackerConnection tc = connections.get(resp.transactionId);
        if (tc == null) {
            logger.warn("Received connect response with unknown transactionId={} from {}",
                    resp.transactionId, from);
            return Optional.empty();
        }

        tc.onConnected(resp.connectionId);

        if (!tc.hasMoreBatches()) {
            logger.info("Session {} has no hashes to scrape – done", tc);
            return Optional.empty();
        }

        return Optional.of(tc.buildNextScrapeRequest());
    }

    protected Optional<byte[]> onAnnounceResponse(AnnounceResponse resp, From from) {
        logger.debug("Tracker → {} from {}", resp, from);
        return Optional.empty();
    }

    /**
     * A tracker has replied to a scrape request.
     * <ol>
     *   <li>Match stats to the pending batch and update torrent activity.</li>
     *   <li>Mark the batch complete.</li>
     *   <li>If more batches remain:
     *     <ul>
     *       <li>Connection still valid → send the next scrape immediately.</li>
     *       <li>Connection expired → reconnect; the new connect request is returned
     *           and the caller sends it back to the tracker.</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    protected Optional<byte[]> onScrapeResponse(ScrapeResponse resp, From from) {
        TrackerConnection tc = connections.get(resp.transactionId);
        if (tc == null) {
            logger.warn("Received scrape response with unknown transactionId={} from {}",
                    resp.transactionId, from);
            return Optional.empty();
        }

        // Process stats for the current in-flight batch.
        if (tc.isBatchPending()) {
            List<String> batch = tc.currentBatch();
            for (int i = 0; i < batch.size(); i++) {
                if (i >= resp.stats.size()) {
                    logger.warn("Scrape response missing stats at index {} for hash {}", i, batch.get(i));
                    continue;
                }
                TorrentStats stats = resp.stats.get(i);
                Torrent torrent = data.torrents.get(batch.get(i));
                if (torrent == null || torrent.meta() == null) continue;
                logger.debug("Scrape stats for {}: {}", batch.get(i), stats);
                boolean active = stats.seeders() > 0 || stats.leechers() > 0;
                int peers = stats.seeders() + stats.leechers();
                torrent.setActive(active);
                data.forUpdate.add(new ImmutablePair<>(torrent.infoHash(), new ImmutablePair<>(torrent.meta().getName(), peers)));
            }
            tc.completeBatch();
        }

        if (!tc.hasMoreBatches()) {
            logger.info("Session {} fully scraped – all batches complete", tc);
            connections.remove(resp.transactionId);
            return Optional.empty();
        }

        // More batches remain – check connection validity before scraping.
        if (tc.isConnectionExpired()) {
            logger.info("Connection ID expired mid-session for {} – reconnecting", tc);
            connections.remove(resp.transactionId);
            tc.reconnect();
            connections.put(tc.transactionId(), tc);
            return Optional.of(tc.connect());
        }

        return Optional.of(tc.buildNextScrapeRequest());
    }

    protected Optional<byte[]> onErrorResponse(ErrorResponse resp, From from) {
        logger.warn("Tracker error from {}: {}", from, resp.message);
        return Optional.empty();
    }
}