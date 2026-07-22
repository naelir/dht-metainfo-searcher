package com.naelir.utp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Extract the IP address string from a socket address.
     */
    private static String unpackAddr(InetSocketAddress addr) {
        if (addr == null || addr.getAddress() == null)
            return null;
        return addr.getAddress().getHostAddress();
    }

    /** Active uTP sessions keyed by remote address + connection-id. */
    private final Map<ConnectionKey, UTPConnection> connections = new HashMap<>();
    // ── Fields ────────────────────────────────────────────────────────────────

    public UTPConnection findConnection(String ip, int port, int connId, int type) {
        ConnectionKey key = new ConnectionKey(ip, port, connId);
        ConnectionKey lookup = key;
        UTPConnection connection = this.connections.get(key);
        if (connection == null && type != UTPConnection.ST_SYN) {
            lookup = new ConnectionKey(ip, port, (connId + 1) & 0xFFFF);
            connection = this.connections.get(lookup);
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
        return Collections.unmodifiableMap(this.connections);
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
        if (senderAddr == null || data == null || data.length < 20)
            return null;
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
                this.connections.entrySet().removeIf(e -> e.getValue() == connection);
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
        List<PendingPacket> toSend = new ArrayList<>();
        for (ConnectionKey key : new ArrayList<>(this.connections.keySet())) {
            UTPConnection utp = this.connections.get(key);
            if (utp == null) {
                continue;
            }
            byte[] res = utp.tick(delta);
            if (res != null && res.length > 0) {
                toSend.add(new PendingPacket(key.ip(), key.port(), res));
            }
            if ("CLOSED".equals(utp.state)) {
                this.connections.remove(key);
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