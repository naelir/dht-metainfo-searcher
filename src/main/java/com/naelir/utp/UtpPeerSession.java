package com.naelir.utp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.ClientHandler;
import com.naelir.bt.HandshakeDecoder;
import com.naelir.bt.HandshakeEncoder;
import com.naelir.bt.Torrent;
import com.naelir.dht.Data;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Bridges a single uTP connection to the BitTorrent peer-wire protocol
 * ({@link HandshakeDecoder} → {@link HandshakeEncoder} → {@link ClientHandler})
 * without requiring a real TCP channel.
 *
 * <h2>How it fits into the uTP pipeline</h2>
 *
 * <pre>
 *  UDP socket (NioDatagramChannel)
 *    └── InboundHandler
 *          └── UtpDataListener → UTPManager → UTPConnection
 *                emit("connected") ──► UtpPeerSession  (construction, triggers channelActive)
 *                emit("data",bytes) ──► UtpPeerSession.feed(bytes)
 *                emit("closed")     ──► UtpPeerSession.close()
 *
 *  UtpPeerSession
 *    ├── inbound:  feed(bytes) ──► EmbeddedChannel ──► HandshakeDecoder ──► ClientHandler
 *    └── outbound: ClientHandler.write(msg) ──► HandshakeEncoder ──► drainOutbound()
 *                        └── sender.accept(encodedBytes) ──► UTPConnection.sendData() ──► UDP
 * </pre>
 *
 * <h2>Why EmbeddedChannel works here</h2>
 * <ul>
 * <li>{@link HandshakeDecoder} is a
 * {@link io.netty.handler.codec.ReplayingDecoder}: it buffers bytes internally
 * across multiple {@link #feed} calls, so fragmented uTP DATA payloads are
 * reassembled exactly as TCP segments would be.</li>
 * <li>All outbound writes by {@link ClientHandler} are captured via
 * {@link EmbeddedChannel#readOutbound()} and forwarded to the {@code sender}
 * callback, which routes them through the live uTP connection.</li>
 * <li>{@link EmbeddedChannel} is created <em>after</em>
 * {@code UTPConnection.emit("connected")} so that {@code channelActive} – which
 * sends the {@link com.naelir.bt.messages.HandshakeMessage} – fires exactly
 * when the uTP session is ready.</li>
 * <li>{@code remoteAddress()} is overridden on the anonymous
 * {@link EmbeddedChannel} subclass so that every
 * {@code ctx.channel().remoteAddress()} call inside {@link ClientHandler}
 * returns the real peer address rather than {@code null}.</li>
 * </ul>
 */
public class UtpPeerSession {
    public static final Logger logger = LogManager.getLogger(UtpPeerSession.class);
    private final EmbeddedChannel embeddedChannel;

    /**
     * Creates the session and immediately fires {@code channelActive} (which sends
     * the BT {@link com.naelir.bt.messages.HandshakeMessage} back through
     * {@code sender}).
     *
     * @param data    application-level shared data (needed by
     *                {@link ClientHandler})
     * @param torrent the torrent whose metadata we want to fetch
     * @param remote  address of the remote uTP peer
     * @param sender  callback that forwards encoded BT bytes into the uTP send
     *                path; called with the raw byte array that must be passed to
     *                {@link UTPConnection#encode(byte[])}
     */
    public UtpPeerSession(Data data, Torrent torrent, InetSocketAddress remote) {
        // Anonymous subclass: override remoteAddress() so ClientHandler's
        // ctx.channel().remoteAddress() sees the real peer InetSocketAddress
        // instead of null (the EmbeddedChannel default).
        this.embeddedChannel = new EmbeddedChannel(new HandshakeEncoder(), new HandshakeDecoder(),
                new ClientHandler(data, torrent)) {
            @Override
            public SocketAddress remoteAddress() {
                return remote;
            }
        };
    }
    // ── Inbound path ──────────────────────────────────────────────────────────

    /**
     * Closes the embedded pipeline (fires {@code channelInactive} on
     * {@link ClientHandler}, giving it a chance to persist partial metadata).
     */
    public void close() {
        this.embeddedChannel.close();
    }
    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Feed a raw application-level payload (the {@code byte[]} from
     * {@code UTPConnection.emit("data", payload)}) into the BT decode pipeline.
     *
     * <p>
     * {@link HandshakeDecoder} will accumulate bytes across calls, matching TCP's
     * streaming behaviour, so callers do not need to worry about message
     * boundaries.
     *
     * @param payload raw bytes emitted by {@link UTPConnection
     */
    public void in(byte[] payload) {
        logger.debug("feeding {} bytes from {} into BT pipeline", payload.length, this.embeddedChannel.remoteAddress());
        if (this.embeddedChannel.isActive()) {
            this.embeddedChannel.writeInbound(Unpooled.wrappedBuffer(payload));
        }
    }

    public Queue<Object> out() {
        return this.embeddedChannel.outboundMessages();
    }
}