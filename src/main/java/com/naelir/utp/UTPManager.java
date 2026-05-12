package com.naelir.utp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java port of {@code Net::uTP::Manager} – multiplexes many uTP sessions over
 * a single UDP socket.
 *
 * <h2>Migration notes</h2>
 * <ul>
 *   <li><b>_unpack_addr</b>: Perl's {@code handle_packet} receives a raw binary
 *       {@code sockaddr} struct from {@code recvfrom()} and decodes it with
 *       {@code Socket::unpack_sockaddr_in[6]} / {@code inet_ntop}.
 *       Java's {@link java.net.DatagramPacket} / NIO
 *       {@link java.nio.channels.DatagramChannel} already exposes the sender as
 *       an {@link InetSocketAddress}, so {@code unpackAddr()} is a trivial
 *       {@link java.net.InetAddress#getHostAddress()} call that handles both
 *       {@link java.net.Inet4Address} and {@link java.net.Inet6Address}.</li>
 *   <li><b>Connection key</b>: Perl uses the string {@code "ip:port:conn_id"}.
 *       That scheme is ambiguous for IPv6 addresses (which contain colons) and
 *       is also fragile because the Perl {@code tick()} does
 *       {@code split(/:/, $key)} to recover ip/port, which would fail for
 *       IPv6.  Java uses the {@link ConnectionKey} record instead.</li>
 *   <li><b>tick() return type</b>: Perl returns an arrayref of hashrefs
 *       ({@code [{ip =>, port =>, data =>}, ...]}).  Java returns a typed
 *       {@code List<PendingPacket>}.</li>
 *   <li><b>event callbacks</b>: Perl anonymous subs reuse {@link UTP.EventCallback}.</li>
 * </ul>
 */
public class UTPManager {

    private static final Logger LOG = Logger.getLogger(UTPManager.class.getName());

    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Immutable connection key replacing Perl's {@code "$ip:$port:$conn_id"} string.
     * Using a record avoids the IPv6-colon-collision bug present in the Perl source.
     */
    public record ConnectionKey(String ip, int port, int connId) {}

    /**
     * A uTP packet that the caller must transmit over UDP.
     * Mirrors one element of the arrayref returned by Perl's {@code tick()}.
     */
    public record PendingPacket(String ip, int port, byte[] data) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Active uTP sessions keyed by remote address + connection-id. */
    private final Map<ConnectionKey, UTP>              connections = new HashMap<>();
    /** Event listeners; mirrors Perl's {@code %on} field. */
    private final Map<String, List<UTP.EventCallback>> on          = new HashMap<>();

    // ── Event system ──────────────────────────────────────────────────────────

    /** Subscribe to a manager event. Supported events: {@code "new_connection"}. */
    public void on(String event, UTP.EventCallback cb) {
        on.computeIfAbsent(event, k -> new ArrayList<>()).add(cb);
    }

    private void emit(String event, Object... args) {
        for (UTP.EventCallback cb : on.getOrDefault(event, Collections.emptyList())) {
            try {
                cb.call(args);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "uTP Manager event '" + event + "' handler threw", e);
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Dispatch an incoming UDP datagram to the correct uTP session.
     *
     * <p>Mirrors Perl's {@code handle_packet($data, $sender_addr)}.
     *
     * <p>If the packet is a {@code ST_SYN} and no session exists for
     * {@code ip:port:conn_id}, a new server-side {@link UTP} instance is created
     * and the {@code "new_connection"} event is emitted with arguments
     * {@code (UTP utp, String ip, int port)}.
     *
     * @param data       raw bytes of the received UDP datagram (≥ 20 bytes)
     * @param senderAddr address the datagram was received from
     * @return response bytes to send back over UDP, or {@code null} if none
     */
    public byte[] handlePacket(byte[] data, InetSocketAddress senderAddr) {
        if (senderAddr == null || data == null || data.length < 20) return null;
        String ip   = unpackAddr(senderAddr);
        int    port = senderAddr.getPort();
        if (ip == null) return null;

        // Peek at vt(1) + ext(1) + conn_id(2) – matching Perl's unpack('C C n', $data)
        ByteBuffer bb     = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.BIG_ENDIAN);
        int        vt     = bb.get() & 0xFF;
        bb.get();                            // ext – consumed but not used here
        int        connId = bb.getShort() & 0xFFFF;
        int        type   = vt & 0x0F;

        ConnectionKey key    = new ConnectionKey(ip, port, connId);
        ConnectionKey lookup = key;             // the key actually holding the UTP
        UTP           utp    = connections.get(key);

        if (utp == null && type != UTP.ST_SYN) {
            // Per BEP-29 / the Perl uTP convention, the client's SYN carries
            // conn_id_recv (= R), while all subsequent client packets carry
            // conn_id_send (= R − 1).  The session was stored under R, so try
            // connId + 1 as a fallback for data / state / fin packets.
            lookup = new ConnectionKey(ip, port, (connId + 1) & 0xFFFF);
            utp    = connections.get(lookup);
        }

        if (utp == null && type == UTP.ST_SYN) {
            // Server side: mirror Perl's conn_id_send => $conn_id + 1, conn_id_recv => $conn_id
            utp = new UTP(connId + 1, connId);
            connections.put(key, utp);
            emit("new_connection", utp, ip, port);
        }

        if (utp != null) {
            byte[] res = utp.receivePacket(data);
            if ("CLOSED".equals(utp.getState())) connections.remove(lookup);
            return res;
        }
        return null;
    }

    /**
     * Allocate a new outgoing uTP connection toward {@code ip:port}.
     *
     * <p>The returned instance is <em>not yet connected</em>.
     * Call {@link UTP#connect()} to get the SYN packet and transmit it:
     * <pre>
     *   UTP conn = manager.newConnection("192.168.1.1", 6881);
     *   byte[] syn = conn.connect();
     *   udpSocket.send(syn, "192.168.1.1", 6881);
     * </pre>
     *
     * @return the new (unconnected) {@link UTP} instance
     */
    public UTP newConnection(String ip, int port) {
        int sendId = new Random().nextInt(65535);
        int recvId = (sendId + 1) & 0xFFFF;
        UTP utp    = new UTP(sendId, recvId);
        // Key by recvId: when the remote responds it echoes recvId as conn_id
        connections.put(new ConnectionKey(ip, port, recvId), utp);
        return utp;
    }

    /**
     * Drive retransmit timers for all active connections.
     * Call this periodically (e.g. every 500 ms) from the event/IO loop.
     *
     * @param delta elapsed seconds since last tick (forwarded to each {@link UTP#tick})
     * @return list of packets that must be sent over UDP by the caller
     */
    public List<PendingPacket> tick(double delta) {
        List<PendingPacket> toSend = new ArrayList<>();
        for (ConnectionKey key : new ArrayList<>(connections.keySet())) {
            UTP    utp = connections.get(key);
            if (utp == null) continue;
            byte[] res = utp.tick(delta);
            if (res != null && res.length > 0) {
                toSend.add(new PendingPacket(key.ip(), key.port(), res));
            }
            if ("CLOSED".equals(utp.getState())) connections.remove(key);
        }
        return toSend;
    }

    /** Read-only snapshot of active connections. */
    public Map<ConnectionKey, UTP> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Extract the IP address string from a socket address.
     *
     * <p>Replaces Perl's {@code _unpack_addr} which decoded a raw binary
     * {@code sockaddr} struct via {@code unpack_sockaddr_in[6]} + {@code inet_ntop}.
     * Java's {@link InetSocketAddress#getAddress()} already handles both IPv4
     * ({@link java.net.Inet4Address}) and IPv6 ({@link java.net.Inet6Address}).
     */
    private static String unpackAddr(InetSocketAddress addr) {
        if (addr == null || addr.getAddress() == null) return null;
        return addr.getAddress().getHostAddress();
    }
}
