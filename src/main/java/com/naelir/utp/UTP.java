package com.naelir.utp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java port of Net::uTP – a minimal μTP (Micro Transport Protocol / BEP-29)
 * implementation with LEDBAT congestion control.
 *
 * <h2>Migration notes</h2>
 * <ul>
 *   <li><b>Header packing</b>: Perl {@code pack 'C C n N N N n n'} maps
 *       directly to a {@link ByteBuffer} with {@link ByteOrder#BIG_ENDIAN}
 *       (network byte order).</li>
 *   <li><b>SACK bitmask</b>: Perl uses {@code pack 'C C V'} where {@code V}
 *       is little-endian 32-bit; preserved here with
 *       {@link ByteOrder#LITTLE_ENDIAN}. {@code _handle_sack} reads the mask
 *       byte-by-byte in both implementations so the semantics are identical.</li>
 *   <li><b>Timestamps</b>: Perl's {@code gettimeofday()} gives true microseconds;
 *       {@link System#currentTimeMillis()} * 1000 gives millisecond precision.
 *       For sub-millisecond accuracy substitute a calibrated
 *       {@link System#nanoTime()} offset.</li>
 *   <li><b>Callbacks</b>: Perl anonymous-sub event listeners become the
 *       {@link EventCallback} functional interface.</li>
 *   <li><b>Byte strings</b>: All Perl byte-string data becomes {@code byte[]}.</li>
 *   <li><b>Unsigned arithmetic</b>: 16-bit fields use {@code & 0xFFFF};
 *       32-bit fields use {@code & 0xFFFFFFFFL} (stored as {@code long}).</li>
 * </ul>
 */
public class UTP {

    private static final Logger LOG = Logger.getLogger(UTP.class.getName());

    // ── Packet types ──────────────────────────────────────────────────────────
    public static final int ST_DATA  = 0;
    public static final int ST_FIN   = 1;
    public static final int ST_STATE = 2;
    public static final int ST_RESET = 3;
    public static final int ST_SYN   = 4;

    // ── Protocol constants ────────────────────────────────────────────────────
    public static final int  VERSION      = 1;
    /** 100 ms expressed in microseconds – LEDBAT target one-way delay. */
    public static final long TARGET_DELAY = 100_000L;

    private static final int HEADER_SIZE = 20;

    // ── Supporting types ──────────────────────────────────────────────────────

    /** Functional interface matching Perl's anonymous-sub event callbacks. */
    @FunctionalInterface
    public interface EventCallback {
        /**
         * Called when the subscribed event fires.
         *
         * <ul>
         *   <li>{@code "connected"} – no args</li>
         *   <li>{@code "closed"}    – no args</li>
         *   <li>{@code "data"}      – {@code args[0]} is {@code byte[]} payload</li>
         * </ul>
         */
        void call(Object... args);
    }

    /** Entry stored in the outgoing-packet retransmit buffer. */
    public static class OutEntry {
        public byte[] data;
        public double ts;      // seconds since epoch (wall clock)
        public int    retries;

        OutEntry(byte[] data, double ts, int retries) {
            this.data    = data;
            this.ts      = ts;
            this.retries = retries;
        }
    }

    /** Decoded uTP packet header. */
    public static class Header {
        public int  vt, ext, connId, seq, ack, type, version;
        public long ts, tDiff, wnd; // unsigned 32-bit values stored as long
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final int connIdSend;
    private final int connIdRecv;

    private String state      = "NEW";   // NEW | CLOSED | SYN_SENT | SYN_RECV | CONNECTED | FIN_SENT | RESET
    private int    seqNr;
    private int    ackNr      = 0;
    private int    windowSize = 1500;
    private int    curWindow  = 0;

    private final List<Long>                 baseDelays = new ArrayList<>();
    private       long                       lastDelay  = 0;

    /** Outgoing retransmit buffer: seq_nr → OutEntry (kept sorted for fast-retransmit). */
    private final TreeMap<Integer, OutEntry> outBuffer  = new TreeMap<>();
    /** In-order receive buffer: seq_nr → payload bytes (for SACK / out-of-order delivery). */
    private final Map<Integer, byte[]>       inBuffer   = new HashMap<>();

    private double rto    = 1.0;
    private double rtt    = 0.0;
    private double rttVar = 0.8;

    private final Map<String, List<EventCallback>> on = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public UTP(int connIdSend, int connIdRecv) {
        this.connIdSend = connIdSend;
        this.connIdRecv = connIdRecv;
        this.seqNr      = new Random().nextInt(65535) + 1;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String                    getState()        { return state; }
    public int                       getAckNr()        { return ackNr; }
    public void                      setAckNr(int v)   { ackNr = v; }
    public Map<Integer, OutEntry>    getOutBuffer()    { return Collections.unmodifiableMap(outBuffer); }
    public Map<Integer, byte[]>      getInBuffer()     { return Collections.unmodifiableMap(inBuffer); }

    // ── Event system ──────────────────────────────────────────────────────────

    /** Subscribe to an event. Multiple listeners per event are supported. */
    public void on(String event, EventCallback cb) {
        on.computeIfAbsent(event, k -> new ArrayList<>()).add(cb);
    }

    private void emit(String event, Object... args) {
        for (EventCallback cb : on.getOrDefault(event, Collections.emptyList())) {
            try {
                cb.call(args);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "uTP event '" + event + "' handler threw", e);
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initiate a connection (client side).
     *
     * @return raw SYN packet bytes to send over UDP
     */
    public byte[] connect() {
        state = "SYN_SENT";
        byte[] pkt = packHeader(ST_SYN, 0, connIdRecv);
        outBuffer.put(seqNr, new OutEntry(pkt, nowSec(), 0));
        seqNr = (seqNr + 1) & 0xFFFF;
        return pkt;
    }

    /**
     * Send application data over an established connection.
     * Fragments {@code data} into 1400-byte chunks automatically.
     *
     * @param data payload bytes
     * @return concatenated raw uTP DATA packets, or {@code null} if not CONNECTED / empty
     */
    public byte[] sendData(byte[] data) {
        if (!"CONNECTED".equals(state) || data == null || data.length == 0) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double now = nowSec();
        int offset = 0;
        while (offset < data.length) {
            int    chunkLen = Math.min(1400, data.length - offset);
            byte[] chunk    = Arrays.copyOfRange(data, offset, offset + chunkLen);
            offset += chunkLen;
            byte[] pkt = concat(packHeader(ST_DATA), chunk);
            outBuffer.put(seqNr, new OutEntry(pkt, now, 0));
            out.write(pkt, 0, pkt.length);
            curWindow += pkt.length;
            seqNr = (seqNr + 1) & 0xFFFF;
        }
        return out.toByteArray();
    }

    /**
     * Drive the retransmission timer. Call periodically (e.g. every 500 ms).
     *
     * @param delta elapsed seconds since last tick (kept for API parity; not used internally)
     * @return packet bytes to retransmit, or a RESET packet if the retry limit is exceeded
     *         (empty array means nothing to resend right now)
     */
    public byte[] tick(double delta) {
        double now = nowSec();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int sn : new ArrayList<>(outBuffer.keySet())) {
            OutEntry entry = outBuffer.get(sn);
            if (entry == null) continue;
            if (now - entry.ts > rto) {
                entry.retries++;
                if (entry.retries > 4) {
                    state = "CLOSED";
                    emit("closed");
                    return packHeader(ST_RESET);
                }
                rto      = Math.min(rto * 2, 30.0);
                entry.ts = now;
                out.write(entry.data, 0, entry.data.length);
                break; // Fast Retransmit: only resend the first timed-out packet
            }
        }
        return out.toByteArray();
    }

    /**
     * Process a received UDP datagram.
     *
     * @param data raw bytes of a single uTP packet
     * @return response packet bytes, or {@code null} if no reply is needed
     */
    public byte[] receivePacket(byte[] data) {
        Header h = unpackHeader(data);
        if (h == null) return null;

        byte[] payload = Arrays.copyOfRange(data, HEADER_SIZE, data.length);

        // ── Parse extensions ───────────────────────────────────────────────
        int extType = h.ext;
        int pos     = 0;
        while (extType != 0 && payload.length - pos >= 2) {
            int nextExt = payload[pos]     & 0xFF;
            int extLen  = payload[pos + 1] & 0xFF;
            if (payload.length - pos < 2 + extLen) {
                LOG.warning("Malformed uTP extension");
                break;
            }
            byte[] extData = Arrays.copyOfRange(payload, pos + 2, pos + 2 + extLen);
            if (extType == 1) handleSack(extData, h.ack); // type 1 = SACK
            pos    += 2 + extLen;
            extType = nextExt;
        }
        if (pos > 0) payload = Arrays.copyOfRange(payload, pos, payload.length);

        // ── LEDBAT congestion control ──────────────────────────────────────
        long nowUs = nowMicros();
        long delay = (nowUs - h.ts) & 0xFFFFFFFFL;
        updateBaseDelay(delay);
        Long minDelay = minBaseDelay();
        if (minDelay != null) {
            long   ourDelay  = (delay - minDelay) & 0xFFFFFFFFL;
            double offTarget = (double) (TARGET_DELAY - ourDelay);
            double adj       = (offTarget / TARGET_DELAY) * 100.0;
            windowSize += (int) adj;
            if (windowSize < 1500) windowSize = 1500;
        }
        lastDelay = delay;

        // ── Cumulative ACKs ────────────────────────────────────────────────
        for (int sn : new ArrayList<>(outBuffer.keySet())) {
            if (((h.ack - sn) & 0xFFFF) < 0x8000) ackPacket(sn);
        }

        // ── State machine ──────────────────────────────────────────────────
        switch (h.type) {

            case ST_SYN:
                if ("NEW".equals(state)) {
                    state = "CONNECTED";
                    ackNr = h.seq;
                    emit("connected");
                    return packHeader(ST_STATE);
                }
                break;

            case ST_STATE:
                if ("SYN_SENT".equals(state)) {
                    state = "CONNECTED";
                    emit("connected");
                }
                ackNr = h.seq;
                break;

            case ST_DATA: {
                int sn = h.seq;
                if (sn == ((ackNr + 1) & 0xFFFF) || ackNr == 0) {
                    ackNr = sn;
                    emit("data", (Object) payload);
                    // Flush contiguous out-of-order buffered segments
                    while (inBuffer.containsKey((ackNr + 1) & 0xFFFF)) {
                        ackNr = (ackNr + 1) & 0xFFFF;
                        byte[] buffered = inBuffer.remove(ackNr);
                        emit("data", (Object) buffered);
                    }
                } else if (((sn - ackNr) & 0xFFFF) < 0x8000 && sn != ackNr) {
                    inBuffer.put(sn, payload); // buffer out-of-order segment
                }
                return packHeader(ST_STATE);
            }

            case ST_RESET:
                state = "CLOSED";
                emit("closed");
                break;

            case ST_FIN:
                state = "CLOSED";
                emit("closed");
                return packHeader(ST_STATE);
        }
        return null;
    }

    // ── Header packing / unpacking ────────────────────────────────────────────

    /**
     * Pack a header using the default {@code connIdSend} and no extension.
     * Equivalent to Perl's {@code $self->pack_header($type)}.
     */
    public byte[] packHeader(int type) {
        return packHeader(type, 0, connIdSend);
    }

    /**
     * Pack a full uTP header.
     *
     * <p>Wire layout (20 bytes, big-endian) mirrors Perl's
     * {@code pack 'C C n N N N n n'}:
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
    public byte[] packHeader(int type, int extension, int connId) {
        byte[] sack = buildSackExtension();
        if (sack.length > 0) extension = 1;

        int vt = (VERSION << 4) | type;
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + sack.length)
                                  .order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) vt);
        bb.put((byte) extension);
        bb.putShort((short) (connId    & 0xFFFF));
        bb.putInt((int)     (nowMicros() & 0xFFFFFFFFL));
        bb.putInt((int)     (lastDelay   & 0xFFFFFFFFL));
        bb.putInt(windowSize);
        bb.putShort((short) (seqNr & 0xFFFF));
        bb.putShort((short) (ackNr & 0xFFFF));
        bb.put(sack);
        return bb.array();
    }

    /**
     * Unpack a raw uTP packet into a {@link Header}.
     *
     * @param data raw packet bytes
     * @return decoded header, or {@code null} if {@code data} is shorter than 20 bytes
     */
    public static Header unpackHeader(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) return null;
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        Header h  = new Header();
        h.vt      = bb.get()      & 0xFF;
        h.ext     = bb.get()      & 0xFF;
        h.connId  = bb.getShort() & 0xFFFF;
        h.ts      = bb.getInt()   & 0xFFFFFFFFL;
        h.tDiff   = bb.getInt()   & 0xFFFFFFFFL;
        h.wnd     = bb.getInt()   & 0xFFFFFFFFL;
        h.seq     = bb.getShort() & 0xFFFF;
        h.ack     = bb.getShort() & 0xFFFF;
        h.type    = h.vt & 0x0F;
        h.version = h.vt >> 4;
        return h;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Build SACK extension bytes.
     *
     * <p>Mirrors Perl's {@code pack 'C C V'}: the 32-bit bitmask is packed
     * <em>little-endian</em> (Perl format letter {@code V}).
     * {@link #handleSack} reads the mask byte-by-byte so endianness is
     * self-consistent within this implementation.
     */
    private byte[] buildSackExtension() {
        if (inBuffer.isEmpty()) return new byte[0];
        int base = (ackNr + 2) & 0xFFFF;
        int mask = 0;
        for (int sn : inBuffer.keySet()) {
            int diff = (sn - base) & 0xFFFF;
            if (diff < 32) mask |= (1 << diff);
        }
        // next_ext=0, len=4, bitmask as little-endian 32-bit (Perl 'V' format)
        ByteBuffer bb = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) 0); // next_ext
        bb.put((byte) 4); // len
        bb.putInt(mask);
        return bb.array();
    }

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

    /** ACK a single packet: update RTT/RTO, shrink curWindow, remove from outBuffer. */
    private void ackPacket(int sn) {
        OutEntry entry = outBuffer.get(sn);
        if (entry == null) return;
        double measuredRtt = nowSec() - entry.ts;
        if (rtt == 0.0) {
            rtt    = measuredRtt;
            rttVar = measuredRtt / 2.0;
        } else {
            final double alpha = 0.125, beta = 0.25;
            rttVar = (1 - beta)  * rttVar + beta  * Math.abs(rtt - measuredRtt);
            rtt    = (1 - alpha) * rtt    + alpha * measuredRtt;
        }
        rto = rtt + rttVar * 4;
        if (rto < 0.5) rto = 0.5;
        curWindow -= entry.data.length;
        outBuffer.remove(sn);
    }

    private void updateBaseDelay(long delay) {
        baseDelays.add(delay);
        if (baseDelays.size() > 60) baseDelays.remove(0);
    }

    private Long minBaseDelay() {
        if (baseDelays.isEmpty()) return null;
        return baseDelays.stream().mapToLong(Long::longValue).min().getAsLong();
    }

    // ── Time helpers ──────────────────────────────────────────────────────────

    /**
     * Microseconds since Unix epoch (millisecond precision).
     * Wrapped to 32 bits to match uTP's 4-byte timestamp field.
     */
    private static long nowMicros() {
        return System.currentTimeMillis() * 1_000L;
    }

    /** Seconds since Unix epoch as a {@code double}, used for RTT/RTO bookkeeping. */
    private static double nowSec() {
        return System.currentTimeMillis() / 1_000.0;
    }

    // ── Byte-array utilities ──────────────────────────────────────────────────

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
