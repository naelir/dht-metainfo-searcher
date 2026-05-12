package com.naelir.utp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.messages.PeerWireMessage;
import com.naelir.bt.messages.ext.IExtendedPeerWireMessage;
import com.naelir.dht.Data;
import com.naelir.dht.IOnDataListener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class UtpClient implements Runnable, AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 17;
    private static final int DEFAULT_TIMEOUT_MS  = 500;
    public static final Logger logger = LogManager.getLogger(UtpClient.class);
    final DatagramSocket socket;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ByteBuffer myself;
    private UtpDataListener listener;
    private Data db;

    public UtpClient(Data data, UtpDataListener listener) throws Exception {
        this.myself = data.myself;
        this.listener = listener;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
        this.db = data;
    }

    @Override
    public void close() {
        stop();
        if (!this.socket.isClosed()) {
            this.socket.close();
        }
    }

    @Override
    public void run() {
        this.running.set(true);
        long lastTickMs = System.currentTimeMillis();
        while (this.running.get()) {
            try {
                byte[] buffer = new byte[this.bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                InetAddress addr = packet.getAddress();
                int port = packet.getPort();
                Optional<byte[]> response = this.listener.onData(data, addr, port);
                if (response.isPresent()) {
                    byte[] encode = response.get();
                    this.socket.send(new DatagramPacket(encode, encode.length, addr, port));
                }
            } catch (SocketTimeoutException e) {
                // Expected: the socket read timed out so we can re-check running.
            } catch (Exception e) {
                if (this.running.get()) {
                    logger.error(e.getMessage(), e);
                }
            }
            // Drive UTP retransmit timers every tick interval
            long nowMs = System.currentTimeMillis();
            double delta = (nowMs - lastTickMs) / 1000.0;
            lastTickMs = nowMs;
            List<UTPManager.PendingPacket> pending = this.listener.getUtpManager().tick(delta);
            for (UTPManager.PendingPacket pp : pending) {
                try {
                    InetAddress tickAddr = InetAddress.getByName(pp.ip());
                    byte[] d = pp.data();
                    this.socket.send(new DatagramPacket(d, d.length, tickAddr, pp.port()));
                } catch (Exception ex) {
                    logger.error("Failed to send UTP tick packet: " + ex.getMessage(), ex);
                }
            }
        }
    }

    public void start() {
        new Thread(this, "utp").start();
    }

    void stop() {
        this.running.set(false);
    }

    /**
     * Serialise {@code message} and send it to the given peer via a uTP session.
     * If no CONNECTED session exists a new uTP connection is initiated and the
     * payload is sent once the handshake completes.
     */
    public void send(PeerWireMessage message, InetAddress addr, int port) throws Exception {
        sendPayloadViaUtp(serializeToBytes(message), addr, port);
    }

    /**
     * Serialise {@code message} and send it to the given peer via a uTP session.
     * If no CONNECTED session exists a new uTP connection is initiated and the
     * payload is sent once the handshake completes.
     */
    public void send(IExtendedPeerWireMessage message, InetAddress addr, int port) throws Exception {
        sendPayloadViaUtp(serializeToBytes(message), addr, port);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static byte[] serializeToBytes(PeerWireMessage message) {
        ByteBuf buf = Unpooled.buffer();
        try {
            message.write(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static byte[] serializeToBytes(IExtendedPeerWireMessage message) {
        ByteBuf buf = Unpooled.buffer();
        try {
            message.write(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    /**
     * Find an existing CONNECTED uTP session for {@code addr:port}, or open a new
     * one, and transmit {@code payload} over it.
     */
    private void sendPayloadViaUtp(byte[] payload, InetAddress addr, int port) throws Exception {
        String ip = addr.getHostAddress();
        UTPManager manager = this.listener.getUtpManager();

        // Re-use an existing CONNECTED session for this peer if available
        UTP existingUtp = null;
        for (Map.Entry<UTPManager.ConnectionKey, UTP> entry : manager.getConnections().entrySet()) {
            UTPManager.ConnectionKey key = entry.getKey();
            if (key.ip().equals(ip) && key.port() == port
                    && "CONNECTED".equals(entry.getValue().getState())) {
                existingUtp = entry.getValue();
                break;
            }
        }

        if (existingUtp != null) {
            byte[] packets = existingUtp.sendData(payload);
            if (packets != null && packets.length > 0) {
                this.socket.send(new DatagramPacket(packets, packets.length, addr, port));
            }
        } else {
            // Open a new uTP connection; defer the data send until "connected" fires
            UTP utp = manager.newConnection(ip, port);
            utp.on("connected", args -> {
                try {
                    byte[] packets = utp.sendData(payload);
                    if (packets != null && packets.length > 0) {
                        this.socket.send(new DatagramPacket(packets, packets.length, addr, port));
                    }
                } catch (Exception ex) {
                    logger.error("Failed to send payload after UTP connected: " + ex.getMessage(), ex);
                }
            });
            byte[] syn = utp.connect();
            if (syn != null && syn.length > 0) {
                this.socket.send(new DatagramPacket(syn, syn.length, addr, port));
            }
        }
    }
}