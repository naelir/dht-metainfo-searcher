package com.naelir.utp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;

/**
 * Java port of Net::uTP – a minimal μTP (Micro Transport Protocol / BEP-29)
 * implementation with LEDBAT congestion control.
 *
 * <h2>Migration notes</h2>
 * <ul>
 * <li><b>Header packing</b>: Perl {@code pack 'C C n N N N n n'} maps directly
 * to a {@link ByteBuffer} with {@link ByteOrder#BIG_ENDIAN} (network byte
 * order).</li>
 * <li><b>SACK bitmask</b>: Perl uses {@code pack 'C C V'} where {@code V} is
 * little-endian 32-bit; preserved here with {@link ByteOrder#LITTLE_ENDIAN}.
 * {@code _handle_sack} reads the mask byte-by-byte in both implementations so
 * the semantics are identical.</li>
 * <li><b>Timestamps</b>: Perl's {@code gettimeofday()} gives true microseconds;
 * {@link System#currentTimeMillis()} * 1000 gives millisecond precision. For
 * sub-millisecond accuracy substitute a calibrated {@link System#nanoTime()}
 * offset.</li>
 * <li><b>Callbacks</b>: Perl anonymous-sub event listeners become the
 * {@link EventCallback} functional interface.</li>
 * <li><b>Byte strings</b>: All Perl byte-string data becomes
 * {@code byte[]}.</li>
 * <li><b>Unsigned arithmetic</b>: 16-bit fields use {@code & 0xFFFF}; 32-bit
 * fields use {@code & 0xFFFFFFFFL} (stored as {@code long}).</li>
 * </ul>
 */
public class UTPConnection {
    public static final Logger logger = LogManager.getLogger(UTPConnection.class);
    // ── Packet types ──────────────────────────────────────────────────────────
    public static final int ST_DATA = 0;
    public static final int ST_FIN = 1;
    public static final int ST_STATE = 2;
    public static final int ST_RESET = 3;
    public static final int ST_SYN = 4;
    // ── Protocol constants ────────────────────────────────────────────────────
    public static final int VERSION = 1;
    /** 100 ms expressed in microseconds – LEDBAT target one-way delay. */
    public static final long TARGET_DELAY = 100_000L;
    private static final int HEADER_SIZE = 20;
    // ── Supporting types ──────────────────────────────────────────────────────

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Microseconds since Unix epoch (millisecond precision). Wrapped to 32 bits to
     * match uTP's 4-byte timestamp field.
     */
    private static long nowMicros() {
        return System.currentTimeMillis() * 1_000L;
    }

    /**
     * Seconds since Unix epoch as a {@code double}, used for RTT/RTO bookkeeping.
     */
    private static double nowSec() {
        return System.currentTimeMillis() / 1_000.0;
    }
    // ── Fields ────────────────────────────────────────────────────────────────

    final int connIdSend;
    final int connIdRecv;
    String state = "NEW"; // NEW | CLOSED | SYN_SENT | SYN_RECV | CONNECTED | FIN_SENT | RESET
    private int seqNr;
    private int ackNr = 0;
    private int windowSize = 1500;
    private int curWindow = 0;
    private final List<Long> baseDelays = new ArrayList<>();
    private long lastDelay = 0;
    /**
     * Outgoing retransmit buffer: seq_nr → OutEntry (kept sorted for
     * fast-retransmit).
     */
    private final TreeMap<Integer, OutEntry> outBuffer = new TreeMap<>();
    /**
     * In-order receive buffer: seq_nr → payload bytes (for SACK / out-of-order
     * delivery).
     */
    private final Map<Integer, byte[]> inBuffer = new HashMap<>();
    private double rto = 1.0;
    private double rtt = 0.0;
    private double rttVar = 0.8;
    private UtpPeerSession session;
    /**
     * Wall-clock time (seconds since epoch) of the last inbound packet or
     * connection creation. Used by {@link UTPManager#tick(double)} to evict
     * connections that never complete a handshake / never send a FIN or RESET
     * and would otherwise sit in {@link UTPManager#connections} forever,
     * causing an unbounded memory leak (e.g. SYN-flood or a peer that silently
     * disappears mid-transfer without ever timing out via the retransmit
     * queue, which only fires when there is outstanding unacked data).
     */
    private double lastActivity = nowSec();
    // ── Constructor ───────────────────────────────────────────────────────────

    public UTPConnection(UtpPeerSession session, int connIdSend, int connIdRecv) {
        this.session = session;
        this.connIdSend = connIdSend;
        this.connIdRecv = connIdRecv;
        this.seqNr = new Random().nextInt(65535);
    }

    /**
     * Close the associated {@link UtpPeerSession} (if any), releasing any
     * unreleased outbound {@link io.netty.buffer.ByteBuf}s and firing
     * {@code channelInactive} on the BT pipeline.
     * Safe to call even when no session is attached (e.g. server-side SYN
     * connections created by {@link UTPManager#findConnection}).
     */
    public void closeSession() {
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
    }

    /**
     * @param idleTimeoutSec how long a connection may go without any inbound
     *                       traffic before it is considered dead.
     * @return {@code true} if no packet has been received (and the connection
     *         wasn't just created) for at least {@code idleTimeoutSec} seconds.
     *         Connections in a terminal state are never reported idle here;
     *         callers should check {@link #state} for {@code CLOSED}
     *         separately.
     */
    public boolean isIdle(double idleTimeoutSec) {
        return (nowSec() - this.lastActivity) > idleTimeoutSec;
    }

    /**
     * ACK a single packet: update RTT/RTO, shrink curWindow, remove from outBuffer.
     */
    private void ackPacket(int sn) {
        OutEntry entry = this.outBuffer.get(sn);
        if (entry == null)
            return;
        double measuredRtt = nowSec() - entry.ts;
        if (this.rtt == 0.0) {
            this.rtt = measuredRtt;
            this.rttVar = measuredRtt / 2.0;
        } else {
            final double alpha = 0.125, beta = 0.25;
            this.rttVar = (1 - beta) * this.rttVar + beta * Math.abs(this.rtt - measuredRtt);
            this.rtt = (1 - alpha) * this.rtt + alpha * measuredRtt;
        }
        this.rto = this.rtt + this.rttVar * 4;
        if (this.rto < 0.5) {
            this.rto = 0.5;
        }
        this.curWindow -= entry.data.length;
        this.outBuffer.remove(sn);
    }

    /**
     * Initiate a connection (client side).
     *
     * @return raw SYN packet bytes to send over UDP
     */
    public byte[] connect() {
        this.state = "SYN_SENT";
        // Spec: "The sequence number is initialized to 1" for ST_SYN
//        seqNr = 1;
        byte[] pkt = packHeader(ST_SYN, 0, this.connIdRecv);
        this.outBuffer.put(this.seqNr, new OutEntry(pkt, nowSec(), 0));
//        seqNr = (seqNr + 1) & 0xFFFF;
        return pkt;
    }

    private byte[] data(Queue<Object> out) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Object o;
            while ((o = out.poll()) != null) {
                if (o instanceof ByteBuf buf) {
                    try {
                        if (buf.readableBytes() > 0) {
                            byte[] bytes = new byte[buf.readableBytes()];
                            buf.readBytes(bytes);
                            outputStream.write(bytes);
                        }
                    } finally {
                        buf.release();
                    }
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Process a received UDP datagram.
     *
     * @param data raw bytes of a single uTP packet
     * @return a {@link DecodeResult} containing the optional response packet,
     *         current state, and optional payload data; never {@code null}
     */
    public DecodeResult decode(byte[] data) {
        this.lastActivity = nowSec();
        Header h = unpackHeader(data);
        if (h == null)
            return new DecodeResult(this.state, null);
        byte[] payload = Arrays.copyOfRange(data, HEADER_SIZE, data.length);
        // ── Parse extensions ───────────────────────────────────────────────
        int extType = h.ext;
        int pos = 0;
        while (extType != 0 && payload.length - pos >= 2) {
            int nextExt = payload[pos] & 0xFF;
            int extLen = payload[pos + 1] & 0xFF;
            if (payload.length - pos < 2 + extLen) {
                logger.warn("Malformed uTP extension");
                break;
            }
            byte[] extData = Arrays.copyOfRange(payload, pos + 2, pos + 2 + extLen);
            if (extType == 1) {
                handleSack(extData, h.ack); // type 1 = SACK
            }
            pos += 2 + extLen;
            extType = nextExt;
        }
        if (pos > 0) {
            payload = Arrays.copyOfRange(payload, pos, payload.length);
        }
        // ── LEDBAT congestion control ──────────────────────────────────────
        long delay = ((nowMicros() & 0xFFFFFFFFL) - h.ts) & 0xFFFFFFFFL;
        updateBaseDelay(delay);
        Long minDelay = minBaseDelay();
        if (minDelay != null) {
            long ourDelay = (delay - minDelay) & 0xFFFFFFFFL;
            double offTarget = TARGET_DELAY - ourDelay;
            double adj = (offTarget / TARGET_DELAY) * 100.0;
            this.windowSize += (int) adj;
            if (this.windowSize < 1500) {
                this.windowSize = 1500;
            }
        }
        this.lastDelay = delay;
        // ── Cumulative ACKs ────────────────────────────────────────────────
        for (int sn : new ArrayList<>(this.outBuffer.keySet())) {
            if (((h.ack - sn) & 0xFFFF) < 0x8000) {
                ackPacket(sn);
            }
        }
        // ── State machine ──────────────────────────────────────────────────
        switch (h.type) {
        case ST_SYN:
            logger.debug("peer {}:{} sent SYN; sending STATE", this.connIdRecv, this.connIdSend);
            if ("NEW".equals(this.state)) {
                this.state = "CONNECTED";
                this.ackNr = h.seq;
                return new DecodeResult(this.state, packHeader(ST_STATE));
            }
            break;
        case ST_STATE:
            logger.debug("peer {}:{} sent STATE; sending STATE, ack={}", this.connIdRecv, this.connIdSend, h.ack);
            if ("SYN_SENT".equals(this.state)) {
                // The spec says ST_STATE (and all packets with no payload) do NOT
                // advance the sender's seq_nr. The accepting end therefore reuses
                // the same seq_nr for its first real ST_DATA. We must acknowledge
                // one *before* the STATE's seq_nr so that the first incoming DATA
                // (seq_nr == h.seq) is accepted as the next in-order packet.
                // This matches libutp's behaviour: ack_nr = pkt.seq_nr - 1.
                this.ackNr = (h.seq - 1) & 0xFFFF;
                this.state = "CONNECTED";
            }
            // During CONNECTED, ST_STATE is normally a pure ACK carrying no
            // payload (h.ack already processed by the cumulative-ACK loop
            // above). However, once the session has application data queued
            // (e.g. our BT handshake becomes available only after the initial
            // SYN/STATE exchange), we must still have a way to push it out -
            // otherwise it sits in the queue forever because the peer may
            // never send ST_DATA to trigger the other flush path, and simply
            // times out / FINs the idle connection. Piggy-back any pending
            // outbound bytes on our STATE reply here.
            if ("CONNECTED".equals(this.state) && this.session != null) {
                Queue<Object> out = this.session.out();
                byte[] raw = data(out);
                if (raw.length > 0) {
                    byte[] response = encode(raw);
                    return new DecodeResult(this.state, response);
                }
            }
            break;
        case ST_DATA: {
            logger.debug("peer {}:{} sent DATA; ack={}", this.connIdRecv, this.connIdSend, h.ack);
            int sn = h.seq;
            byte[] deliveredData = new byte[0];
            if (sn == ((this.ackNr + 1) & 0xFFFF) || this.ackNr == 0) {
                this.ackNr = sn;
                ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
                dataOut.write(payload, 0, payload.length);
                // Flush contiguous out-of-order buffered segments
                while (this.inBuffer.containsKey((this.ackNr + 1) & 0xFFFF)) {
                    this.ackNr = (this.ackNr + 1) & 0xFFFF;
                    byte[] buffered = this.inBuffer.remove(this.ackNr);
                    dataOut.write(buffered, 0, buffered.length);
                }
                deliveredData = dataOut.toByteArray();
            } else if (((sn - this.ackNr) & 0xFFFF) < 0x8000 && sn != this.ackNr) {
                this.inBuffer.put(sn, payload); // buffer out-of-order segment
                logger.debug("out of order packet");
            }
            if (this.session == null) {
                // No BT session attached (e.g. server-side connection we never
                // accept). Still ACK the data so the remote doesn't keep
                // retransmitting, but there is nothing to feed/drain.
                return new DecodeResult(this.state, packHeader(ST_STATE));
            }
            this.session.in(deliveredData);
            Queue<Object> out = this.session.out();
            byte[] raw = data(out);
            byte[] response = encode(raw);
            byte[] merge = merge(packHeader(ST_STATE), response);
            return new DecodeResult(this.state, merge);
        }
        case ST_RESET:
            logger.debug("peer {}:{} sent RESET; closing connection", this.connIdRecv, this.connIdSend);
            this.state = "CLOSED";
            break;
        case ST_FIN:
            logger.debug("peer {}:{} sent FIN; closing connection", this.connIdRecv, this.connIdSend);
            this.state = "CLOSED";
            return new DecodeResult(this.state, packHeader(ST_STATE));
        }
        return new DecodeResult(this.state, null);
    }

    /**
     * Send application data over an established connection. Fragments {@code data}
     * into 1400-byte chunks automatically.
     *
     * @param data payload bytes
     * @return concatenated raw uTP DATA packets, or {@code null} if not CONNECTED /
     *         empty
     */
    public byte[] encode(byte[] data) {
        if (!"CONNECTED".equals(this.state) || data == null || data.length == 0)
            return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double now = nowSec();
        int offset = 0;
        while (offset < data.length) {
            int chunkLen = Math.min(1400, data.length - offset);
            byte[] chunk = Arrays.copyOfRange(data, offset, offset + chunkLen);
            offset += chunkLen;
            this.seqNr = (this.seqNr + 1) & 0xFFFF;
//            ackNr = (ackNr - 1) & 0xFFFF; // it is accurate, but why?
            byte[] pkt = concat(packHeader(ST_DATA), chunk);
            this.outBuffer.put(this.seqNr, new OutEntry(pkt, now, 0));
            out.write(pkt, 0, pkt.length);
            this.curWindow += pkt.length;
        }
        return out.toByteArray();
    }
    // ── Header packing / unpacking ────────────────────────────────────────────

    private void handleSack(byte[] bitmask, int ackNrIn) {
        int base = (ackNrIn + 2) & 0xFFFF;
        for (int i = 0; i < bitmask.length; i++) {
            int b = bitmask[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                if ((b & (1 << bit)) != 0) {
                    ackPacket((base + i * 8 + bit) & 0xFFFF);
                }
            }
        }
    }

    private byte[] merge(byte[]... arrays) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            for (byte[] array : arrays) {
                outputStream.write(array);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private Long minBaseDelay() {
        if (this.baseDelays.isEmpty())
            return null;
        return this.baseDelays.stream().mapToLong(Long::longValue).min().getAsLong();
    }
    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Pack a header using the default {@code connIdSend} and no extension.
     * Equivalent to Perl's {@code $self->pack_header($type)}.
     */
    byte[] packHeader(int type) {
        return packHeader(type, 0, this.connIdSend);
    }

    /**
     * Pack a full uTP header.
     *
     * <p>
     * Wire layout (20 bytes, big-endian) mirrors Perl's
     * {@code pack 'C C n N N N n n'}:
     *
     * <pre>
     *  0       vt         (version&lt;&lt;4 | type)   1 byte
     *  1       extension                          1 byte
     *  2–3     conn_id                            2 bytes (unsigned)
     *  4–7     timestamp                          4 bytes (unsigned)
     *  8–11    last_delay                         4 bytes (unsigned)
     *  12–15   window_size                        4 bytes (unsigned)
     *  16–17   seq_nr                             2 bytes (unsigned)
     *  18–19   ack_nr                             2 bytes (unsigned)
     *  20+     SACK extension (optional)
     * </pre>
     */
    byte[] packHeader(int type, int extension, int connId) {
//        byte[] sack = buildSackExtension();
//        if (sack.length > 0) extension = 1;
        // Spec §"header format": byte 0 = | type (high nibble) | ver (low nibble) |
        int vt = (type << 4) | VERSION;
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE/* + sack.length */).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) vt);
        bb.put((byte) extension);
        bb.putShort((short) (connId & 0xFFFF));
        bb.putInt((int) (nowMicros() & 0xFFFFFFFFL));
        bb.putInt((int) (this.lastDelay & 0xFFFFFFFFL));
        bb.putInt(this.windowSize);
        bb.putShort((short) (this.seqNr & 0xFFFF));
        bb.putShort((short) (this.ackNr & 0xFFFF));
//        bb.put(sack);
        return bb.array();
    }

    /**
     * Drive the retransmission timer. Call periodically (e.g. every 500 ms).
     *
     * @param delta elapsed seconds since last tick (kept for API parity; not used
     *              internally)
     * @return packet bytes to retransmit, or a RESET packet if the retry limit is
     *         exceeded (empty array means nothing to resend right now)
     */
    public byte[] tick(double delta) {
        double now = nowSec();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int sn : new ArrayList<>(this.outBuffer.keySet())) {
            OutEntry entry = this.outBuffer.get(sn);
            if (entry == null) {
                continue;
            }
            if (now - entry.ts > this.rto) {
                entry.retries++;
                if (entry.retries > 4) {
                    this.state = "CLOSED";
                    return packHeader(ST_RESET);
                }
                this.rto = Math.min(this.rto * 2, 30.0);
                entry.ts = now;
                out.write(entry.data, 0, entry.data.length);
                break; // Fast Retransmit: only resend the first timed-out packet
            }
        }
        return out.toByteArray();
    }

    /**
     * Unpack a raw uTP packet into a {@link Header}.
     *
     * @param data raw packet bytes
     * @return decoded header, or {@code null} if {@code data} is shorter than 20
     *         bytes
     */
    Header unpackHeader(byte[] data) {
        if (data == null || data.length < HEADER_SIZE)
            return null;
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        Header h = new Header();
        h.vt = bb.get() & 0xFF;
        h.ext = bb.get() & 0xFF;
        h.connId = bb.getShort() & 0xFFFF;
        h.ts = bb.getInt() & 0xFFFFFFFFL;
        h.tDiff = bb.getInt() & 0xFFFFFFFFL;
        h.wnd = bb.getInt() & 0xFFFFFFFFL;
        h.seq = bb.getShort() & 0xFFFF;
        h.ack = bb.getShort() & 0xFFFF;
        // Spec §"header format": high nibble = type, low nibble = version
        h.type = h.vt >> 4;
        h.version = h.vt & 0x0F;
        return h;
    }

    private void updateBaseDelay(long delay) {
        this.baseDelays.add(delay);
        if (this.baseDelays.size() > 60) {
            this.baseDelays.remove(0);
        }
    }
    
    
    // ── Time helpers ──────────────────────────────────────────────────────────

    /**
     * Result returned by {@link #decode(byte[])}.
     *
     * <ul>
     * <li>{@link #responsePacket} – optional ST_STATE (or other) reply to send back
     * over UDP; {@code null} if no reply is needed.</li>
     * <li>{@link #state} – connection state after processing the packet.</li>
     * <li>{@link #data} – optional application payload; {@code null} if the packet
     * carried no data (e.g. pure ACK or SYN/STATE handshake).</li>
     * </ul>
     */
    public record DecodeResult(String state, byte[] response) {
    }

    /** Decoded uTP packet header. */
    public static class Header {
        public int vt, ext, connId, seq, ack, type, version;
        public long ts, tDiff, wnd; // unsigned 32-bit values stored as long
    }
    // ── Byte-array utilities ──────────────────────────────────────────────────

    /** Entry stored in the outgoing-packet retransmit buffer. */
    public class OutEntry {
        public byte[] data;
        public double ts; // seconds since epoch (wall clock)
        public int retries;

        OutEntry(byte[] data, double ts, int retries) {
            this.data = data;
            this.ts = ts;
            this.retries = retries;
        }
    }
}
