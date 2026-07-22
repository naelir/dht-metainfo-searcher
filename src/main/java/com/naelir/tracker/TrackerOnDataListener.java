package com.naelir.tracker;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.From;

/**
 * Parses incoming UDP datagrams that belong to the BitTorrent UDP tracker
 * protocol (BEP-15) and dispatches them to the appropriate handler methods.
 *
 * <p>
 * Detection rules (checked in order):
 * <ol>
 * <li>If the first 8 bytes equal {@link ConnectRequest#PROTOCOL_ID} → {@link ConnectRequest}.</li>
 * <li>Otherwise read the 4-byte action at offset 0 and dispatch:
 *   <ul>
 *     <li>0 → {@link ConnectResponse}</li>
 *     <li>1 → {@link AnnounceResponse}</li>
 *     <li>2 → {@link ScrapeResponse}</li>
 *     <li>3 → {@link ErrorResponse}</li>
 *   </ul>
 * </li>
 * </ol>
 *
 * Override the {@code on*} methods to add application-level behaviour.
 * The default implementations just log the received message.
 */
public class TrackerOnDataListener {
    public static final Logger logger = LogManager.getLogger(TrackerOnDataListener.class);

    /**
     * Entry point called by the network layer for every incoming UDP datagram
     * that has been identified as a tracker protocol packet.
     *
     * @param buffer  raw datagram bytes
     * @param address sender's IP address
     * @param port    sender's UDP port
     * @return an optional response to send back to the sender
     */
    public Optional<byte[]> onData(byte[] buffer, InetAddress address, int port) {
        From from = new From(address.getAddress(), port);
        try {
            if (ConnectRequest.matches(buffer)) {
                ConnectRequest req = ConnectRequest.decode(buffer);
                return onConnectRequest(req, from);
            }

            if (buffer.length < 4) {
                logger.warn("Tracker packet from {} too short ({} bytes)", from, buffer.length);
                return Optional.empty();
            }

            int action = ByteBuffer.wrap(buffer, 0, 4).getInt();
            return switch (action) {
                case 0 -> onConnectResponse(ConnectResponse.decode(buffer), from);
                case 1 -> onAnnounceResponse(AnnounceResponse.decode(buffer), from);
                case 2 -> onScrapeResponse(ScrapeResponse.decode(buffer), from);
                case 3 -> onErrorResponse(ErrorResponse.decode(buffer), from);
                default -> {
                    logger.warn("Unknown tracker action {} from {}", action, from);
                    yield Optional.empty();
                }
            };
        } catch (Exception e) {
            logger.error("Failed to parse tracker packet from {}: {}", from, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Returns {@code true} when {@code data} looks like a UDP tracker protocol packet.
     *
     * <p>A datagram is considered a tracker packet when:
     * <ul>
     *   <li>It starts with the 8-byte connect magic ({@link ConnectRequest#PROTOCOL_ID}), <em>or</em></li>
     *   <li>It is at least 4 bytes long and the first 4 bytes encode an action code in [0, 3]
     *       that is <em>not</em> consistent with a uTP header (i.e. the byte at offset 0 is
     *       {@code 0x00}, {@code 0x01}, {@code 0x02}, or {@code 0x03}).</li>
     * </ul>
     *
     * @param data raw UDP payload
     * @return {@code true} if the datagram matches a tracker protocol packet
     */
    public static boolean isTrackerPacket(byte[] data) {
        if (data == null || data.length < 4)
            return false;
        // A connect request starts with the 8-byte magic constant.
        if (data.length >= 16 && ConnectRequest.matches(data))
            return true;
        // A response starts with a 4-byte action in {0,1,2,3}.
        // Action 0 (connect response) is 16 bytes, 1 (announce) is 20+, 2 (scrape) is 8+, 3 (error) is 8+.
        int action = ByteBuffer.wrap(data, 0, 4).getInt();
        return action >= 0 && action <= 3 && data[0] == 0x00;
    }

    // ── Overridable handlers ──────────────────────────────────────────────────

    protected Optional<byte[]> onConnectRequest(ConnectRequest req, From from) {
        logger.debug("Tracker ← {} from {}", req, from);
        return Optional.empty();
    }

    protected Optional<byte[]> onConnectResponse(ConnectResponse resp, From from) {
        logger.debug("Tracker → {} from {}", resp, from);
        return Optional.empty();
    }

    protected Optional<byte[]> onAnnounceResponse(AnnounceResponse resp, From from) {
        logger.debug("Tracker → {} from {}", resp, from);
        return Optional.empty();
    }

    protected Optional<byte[]> onScrapeResponse(ScrapeResponse resp, From from) {
        logger.debug("Tracker → {} from {}", resp, from);
        return Optional.empty();
    }

    protected Optional<byte[]> onErrorResponse(ErrorResponse resp, From from) {
        logger.warn("Tracker error from {}: {}", from, resp.message);
        return Optional.empty();
    }
}