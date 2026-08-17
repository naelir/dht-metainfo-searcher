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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

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
     * <p>
     * Any {@link ByteBuf}s that were encoded and placed in the outbound queue but
     * never consumed (e.g. the BT handshake produced during {@code channelActive}
     * if the uTP peer never replied) are explicitly released here to prevent
     * Netty's {@code ResourceLeakDetector} from reporting a leak.
     */
    public void close() {
        // Drain and release any ByteBufs left in the outbound queue before
        // closing so they are not garbage-collected without being released.
        Queue<Object> outbound = this.embeddedChannel.outboundMessages();
        Object msg;
        while ((msg = outbound.poll()) != null) {
            if (msg instanceof ByteBuf buf) {
                buf.release();
            }
        }
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