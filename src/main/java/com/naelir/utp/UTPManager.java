package com.naelir.utp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Java port of {@code Net::uTP::Manager} – multiplexes many uTP sessions over a
 * single UDP socket.
 *
 * <h2>Migration notes</h2>
 * <ul>
 * <li><b>_unpack_addr</b>: Perl's {@code handle_packet} receives a raw binary
 * {@code sockaddr} struct from {@code recvfrom()} and decodes it with
 * {@code Socket::unpack_sockaddr_in[6]} / {@code inet_ntop}. Java's
 * {@link java.net.DatagramPacket} / NIO
 * {@link java.nio.channels.DatagramChannel} already exposes the sender as an
 * {@link InetSocketAddress}, so {@code unpackAddr()} is a trivial
 * {@link java.net.InetAddress#getHostAddress()} call that handles both
 * {@link java.net.Inet4Address} and {@link java.net.Inet6Address}.</li>
 * <li><b>Connection key</b>: Perl uses the string {@code "ip:port:conn_id"}.
 * That scheme is ambiguous for IPv6 addresses (which contain colons) and is
 * also fragile because the Perl {@code tick()} does {@code split(/:/, $key)} to
 * recover ip/port, which would fail for IPv6. Java uses the
 * {@link ConnectionKey} record instead.</li>
 * <li><b>tick() return type</b>: Perl returns an arrayref of hashrefs
 * ({@code [{ip =>, port =>, data =>}, ...]}). Java returns a typed
 * {@code List<PendingPacket>}.</li>
 * <li><b>event callbacks</b>: Replaced by {@link UTPConnection.DecodeResult}
 * returned from {@link UTPConnection#decode(byte[])}.</li>
 * </ul>
 */
public class UTPManager {
    public static final Logger logger = LogManager.getLogger(UTPManager.class);
    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Extract the IP address string from a socket address.
     */
    private static String unpackAddr(InetSocketAddress addr) {
        if (addr == null || addr.getAddress() == null)
            return null;
        return addr.getAddress().getHostAddress();
    }

    /**
     * How long a connection may sit without receiving any packet before it is
     * forcibly evicted. Without this, connections that stall after a
     * handshake (or a flood of bogus ST_SYN packets that are never followed
     * up) would accumulate forever: {@link UTPConnection#tick} only closes a
     * connection when it has *unacked outstanding data* that exceeds the
     * retry limit, so an idle connection with an empty retransmit queue would
     * otherwise never be removed — a genuine memory leak.
     */
    private static final long IDLE_TIMEOUT_SEC = 180L;

    /**
     * Hard upper bound on the number of concurrently tracked connections. Acts
     * as a backstop against SYN-flood style attacks that create many
     * {@link UTPConnection} instances faster than the idle timeout can reap
     * them; Guava evicts least-recently-used entries once this is exceeded.
     */
    private static final long MAX_CONNECTIONS = 20_000L;

    /**
     * Active uTP sessions keyed by remote address + connection-id.
     *
     * <p>
     * Backed by a Guava {@link Cache} instead of a plain
     * {@code ConcurrentHashMap} so that stale/abandoned connections are bounded
     * automatically:
     * <ul>
     * <li>{@code expireAfterAccess} reclaims connections that stop receiving
     * traffic (idle peers, half-open handshakes, dropped sessions) without
     * relying solely on the manual sweep in {@link #tick(double)}.</li>
     * <li>{@code maximumSize} caps total memory use even under a sustained
     * SYN-flood where new keys are created faster than they can go idle.</li>
     * <li>The {@link RemovalListener} guarantees {@link UTPConnection#closeSession()}
     * is always invoked, releasing any attached {@link UtpPeerSession} /
     * Netty buffers, regardless of *why* the entry was removed (expiry, size
     * eviction, or explicit invalidation).</li>
     * </ul>
     */
    private final Cache<ConnectionKey, UTPConnection> connections = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(IDLE_TIMEOUT_SEC))
            .maximumSize(MAX_CONNECTIONS)
            .removalListener((RemovalListener<ConnectionKey, UTPConnection>) this::onRemoval)
            .build();
    // ── Fields ────────────────────────────────────────────────────────────────

    private void onRemoval(RemovalNotification<ConnectionKey, UTPConnection> notification) {
        UTPConnection utp = notification.getValue();
        if (utp != null) {
            utp.closeSession();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("uTP connection " + notification.getKey() + " removed: " + notification.getCause());
        }
    }

    public UTPConnection findConnection(String ip, int port, int connId, int type) {
        ConnectionKey key = new ConnectionKey(ip, port, connId);
        UTPConnection connection = this.connections.getIfPresent(key);
        if (connection == null && type != UTPConnection.ST_SYN) {
            // Depending on whether the cached entry was created as the
            // *initiator* (stored under recvId) or as the *acceptor* of an
            // incoming ST_SYN (stored under the peer's seed id, one less
            // than our own recvId), the id actually carried by subsequent
            // packets can be offset by +1 in either direction relative to
            // the stored key. Try both to avoid spuriously losing the
            // connection ("no connection found") right after the handshake.
            ConnectionKey plusOne = new ConnectionKey(ip, port, (connId + 1) & 0xFFFF);
            connection = this.connections.getIfPresent(plusOne);
            if (connection == null) {
                ConnectionKey minusOne = new ConnectionKey(ip, port, (connId - 1) & 0xFFFF);
                connection = this.connections.getIfPresent(minusOne);
            }
        }
        if (connection == null && type == UTPConnection.ST_SYN) {
            connection = new UTPConnection(null, connId, connId + 1);
            this.connections.put(key, connection);
        }
        return connection;
    }
    // ── Public API ────────────────────────────────────────────────────────────

    /** Read-only snapshot of active connections. */
    public Map<ConnectionKey, UTPConnection> getConnections() {
        return Collections.unmodifiableMap(this.connections.asMap());
    }

    /**
     * Dispatch an incoming UDP datagram to the correct uTP session.
     *
     * <p>
     * Mirrors Perl's {@code handle_packet($data, $sender_addr)}.
     *
     * <p>
     * If the packet is a {@code ST_SYN} and no session exists for
     * {@code ip:port:conn_id}, a new server-side {@link UTPConnection} instance is
     * created.
     *
     * @param data       raw bytes of the received UDP datagram (≥ 20 bytes)
     * @param senderAddr address the datagram was received from
     * @return a {@link HandleResult} with an optional response packet and optional
     *         payload, or {@code null} if the datagram was invalid
     */
    public byte[] handlePacket(byte[] data, InetSocketAddress senderAddr) {
        if (senderAddr == null || data == null || data.length < 20) {
            return null;
        }
        String ip = unpackAddr(senderAddr);
        int port = senderAddr.getPort();
        if (ip == null)
            return null;
        // Peek at vt(1) + ext(1) + conn_id(2) – matching Perl's unpack('C C n', $data)
        ByteBuffer bb = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.BIG_ENDIAN);
        int vt = bb.get() & 0xFF;
        bb.get(); // ext – consumed but not used here
        int connId = bb.getShort() & 0xFFFF;
        // Spec §"header format": high nibble = type, low nibble = version
        int type = vt >> 4;
        UTPConnection connection = findConnection(ip, port, connId, type);
        if (connection != null) {
            UTPConnection.DecodeResult res = connection.decode(data);
            if ("CLOSED".equals(res.state())) {
                this.connections.asMap().values().removeIf(v -> v == connection);
                logger.debug("{}: {}, {}/{} connection closed", ip, port, connection.connIdRecv, connection.connIdSend);
                connection.closeSession();
            }
            return res.response();
        }
        return null;
    }

    /**
     * Allocate a new outgoing uTP connection toward {@code ip:port}.
     *
     * <p>
     * The returned instance is <em>not yet connected</em>. Call
     * {@link UTPConnection#connect()} to get the SYN packet and transmit it:
     *
     * <pre>
     * UTPConnection conn = manager.newConnection("192.168.1.1", 6881);
     * byte[] syn = conn.connect();
     * udpSocket.send(syn, "192.168.1.1", 6881);
     * </pre>
     *
     * @param session
     *
     * @return the new (unconnected) {@link UTPConnection} instance
     */
    public UTPConnection newConnection(UtpPeerSession session, String ip, int port) {
        int recvId = new Random().nextInt(65535);
        int sendId = (recvId + 1) & 0xFFFF;
        UTPConnection utp = new UTPConnection(session, sendId, recvId);
        this.connections.put(new ConnectionKey(ip, port, recvId), utp);
        return utp;
    }

    /**
     * Drive retransmit timers for all active connections. Call this periodically
     * (e.g. every 500 ms) from the event/IO loop.
     *
     * @param delta elapsed seconds since last tick (forwarded to each
     *              {@link UTPConnection#tick})
     * @return list of packets that must be sent over UDP by the caller
     */
    public List<PendingPacket> tick(double delta) {
        // Force Guava to process any pending expiration/size-based evictions
        // (and fire the removalListener) even if no get()/put() happened on
        // those particular entries recently.
        this.connections.cleanUp();
        List<PendingPacket> toSend = new ArrayList<>();
        for (Map.Entry<ConnectionKey, UTPConnection> entry : new ArrayList<>(this.connections.asMap().entrySet())) {
            ConnectionKey key = entry.getKey();
            UTPConnection utp = entry.getValue();
            if (utp == null) {
                continue;
            }
            if (utp.isIdle(IDLE_TIMEOUT_SEC)) {
                this.connections.invalidate(key);
                continue;
            }
            byte[] res = utp.tick(delta);
            if (res != null && res.length > 0) {
                toSend.add(new PendingPacket(key.ip(), key.port(), res));
            }
            if ("CLOSED".equals(utp.state)) {
                this.connections.invalidate(key);
            }
        }
        return toSend;
    }

    /**
     * Immutable connection key replacing Perl's {@code "$ip:$port:$conn_id"}
     * string. Using a record avoids the IPv6-colon-collision bug present in the
     * Perl source.
     */
    public record ConnectionKey(String ip, int port, int connId) {
    }
    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * A uTP packet that the caller must transmit over UDP. Mirrors one element of
     * the arrayref returned by Perl's {@code tick()}.
     */
    public record PendingPacket(String ip, int port, byte[] data) {
    }
}