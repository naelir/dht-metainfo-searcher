package com.naelir.utp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.AnnouncePeerRequest;
import com.naelir.dht.BEncoder;
import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.FindNodeRequest;
import com.naelir.dht.From;
import com.naelir.dht.Generator;
import com.naelir.dht.GetPeersRequest;
import com.naelir.dht.IRequest;
import com.naelir.dht.Node;
import com.naelir.dht.PingRequest;
import com.naelir.dht.SampleInfoHashesRequest;
import com.naelir.dht.Token;
import com.naelir.tracker.TrackerUdpManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
 
/**
 * A Netty-based UDP client that mirrors {@link UtpClient} but uses Netty's
 * {@link NioDatagramChannel} and event-loop infrastructure instead of a plain
 * {@link java.net.DatagramSocket}.
 *
 * <p>
 * Incoming datagrams are processed by an inlined
 * {@link ChannelInboundHandlerAdapter}; the UTP tick is driven by a recurring
 * task submitted to the Netty event loop so that no extra thread is needed.
 */
public class UtpClient implements AutoCloseable {
    public static final long TICK_INTERVAL_MS = 500;
    public static final Logger logger = LogManager.getLogger(UtpClient.class);

    /**
     * Returns {@code true} when {@code data} looks like a valid uTP datagram.
     *
     * <p>
     * The uTP header (BEP-29 §3) is always exactly 20 bytes. Byte 0 encodes both
     * the <em>packet type</em> (high nibble) and the <em>version</em> (low nibble)
     * as {@code (type << 4) | version}:
     *
     * <pre>
     *  bit 7-4  type     0=ST_DATA  1=ST_FIN  2=ST_STATE  3=ST_RESET  4=ST_SYN
     *  bit 3-0  version  must be 1
     * </pre>
     *
     * <p>
     * Three necessary (but not sufficient) conditions are checked:
     * <ol>
     * <li>Payload is at least 20 bytes (minimum header size).</li>
     * <li>Low nibble of byte 0 equals 1 (version == 1).</li>
     * <li>High nibble of byte 0 is in [0, 4] (known packet types only).</li>
     * </ol>
     *
     * @param data raw UDP payload bytes
     * @return {@code true} if the datagram matches the uTP header signature
     */
    public static boolean isUtpPacket(byte[] data) {
        if (data == null || data.length < 20)
            return false;
        int vt = data[0] & 0xFF;
        int version = vt & 0x0F; // low nibble
        int type = vt >> 4; // high nibble
        return version == UTPConnection.VERSION // version must be 1
                && type <= UTPConnection.ST_SYN; // type in { 0, 1, 2, 3, 4 }
    }

    private volatile Channel channel;
    private Data data;
    private UTPManager utpManager;
    private TrackerUdpManager trackerUdpManager;
    
    public UtpClient(Channel channel, Data data, UTPManager utpManager, TrackerUdpManager trackerUdpManager) {
        this.data = data;
        this.channel = channel;
        this.utpManager = utpManager;
        this.trackerUdpManager = trackerUdpManager;
    }

    @Override
    public void close() {//
    }

    /**
     * Establishes a uTP connection to {@code addr:port} and attaches a full
     * BitTorrent peer-wire pipeline ({@link com.naelir.bt.HandshakeDecoder} →
     * {@link com.naelir.bt.HandshakeEncoder} → {@link com.naelir.bt.ClientHandler})
     * to it via {@link UtpPeerSession}.
     *
     * <p>
     * Event wiring:
     * <ul>
     * <li>{@code "connected"} — {@link UtpPeerSession} is constructed, which fires
     * {@code channelActive} and sends the BT handshake immediately.</li>
     * <li>{@code "data"} — raw payload is fed into the embedded BT pipeline; any BT
     * response bytes are encoded and sent back through the uTP connection.</li>
     * <li>{@code "closed"} — {@link UtpPeerSession#close()} is called, which fires
     * {@code channelInactive} on {@link com.naelir.bt.ClientHandler}.</li>
     * </ul>
     *
     * @param torrent the torrent whose {@code infoHash} is used in the BT handshake
     * @param addr    remote peer address
     * @param port    remote peer port
     */
    public void connectPeer(Torrent torrent, InetAddress addr, int port) throws Exception {
        String ip = addr.getHostAddress();
        InetSocketAddress remote = new InetSocketAddress(addr, port);
        UtpPeerSession session = new UtpPeerSession(this.data, torrent, remote);
        UTPConnection connection = utpManager.newConnection(session, ip, port);
        // Send the uTP SYN to start the handshake.
        byte[] syn = connection.connect();
        if (syn != null && syn.length > 0) {
            writeUdp(syn, addr, port);
        }
    }

    public void connectPeer(Torrent torrent, Node node) throws Exception {
        connectPeer(torrent, node.address(), node.port());
    }

    public void scrape(Set<String> hashes, InetAddress addr, int port) throws Exception {
        byte[] encode = trackerUdpManager.newConnection(hashes, addr.getHostAddress(), port);
        if (encode != null && encode.length > 0) {
            writeUdp(encode, addr, port);
        }
    }

    private List<Node> contactPoints() throws UnknownHostException {
//      byte[] byName1 = InetAddress.getByName("router.bittorrent.com").getAddress();
        byte[] byName2 = InetAddress.getByName("dht.transmissionbt.com").getAddress();
//      byte[] byName3 = InetAddress.getByName("router.utorrent.com").getAddress();
        return List.of(/* new Node(byName1, 6881), */new Node(byName2, 6881)/* , new Node(byName3, 6881) */);
    }

    public void explore(ByteBuffer myself, List<Node> closest) throws Exception {
        if (closest.isEmpty() == false) {
            int fails = 0;
            for (Node node : closest) {
                try {
                    sendFindNode(myself, myself, node);
                } catch (Exception e) {
                    fails++;
                    logger.error(e.getMessage());
                }
            }
            if (fails == closest.size()) {
                List<Node> contactPoints = contactPoints();
                for (Node node : contactPoints) {
                    sendFindNode(myself, myself, node);
                }
            }
        } else {
            List<Node> contactPoints = contactPoints();
            for (Node node : contactPoints) {
                sendFindNode(myself, myself, node);
            }
        }
    }

    private void logTo(Object decode, From from) {
        logger.debug("{}, {} to {}, port {}", decode.getClass().getSimpleName(), decode, Generator.inet(from.ip), from.port);
    }

    void send(IRequest request, InetAddress addr, int port) throws Exception {
        From from = new From(addr.getAddress(), port);
        logTo(request, from);
        byte[] encode = BEncoder.encode(request);
        if (port > 0) {
            this.data.requestsSent.put(request.tid(), request);
            writeUdp(encode, addr, port);
        }
    }

    public void sendAnnouncePeer(ByteBuffer myself, ByteBuffer torrent, Node node, int port) throws Exception {
        Token token = new Token(node.ip());
        AnnouncePeerRequest r = new AnnouncePeerRequest(myself, torrent, token.value, port, node);
        node.put(Command.ANNOUNCE);
        send(r, node.address(), node.port());
    }

    public void sendFindNode(ByteBuffer myself, ByteBuffer id, Node node) throws Exception {
        FindNodeRequest r = new FindNodeRequest(myself, id, node);
        node.put(Command.FIND_NODE);
        send(r, node.address(), node.port());
    }

    public void sendGetPeers(ByteBuffer myself, ByteBuffer torrent, Node node) throws Exception {
        GetPeersRequest r = new GetPeersRequest(myself, torrent, node);
        node.put(Command.GET_PEER);
        send(r, node.address(), node.port());
    }

    public void sendPing(ByteBuffer myself, Node node) throws Exception {
        PingRequest r = new PingRequest(myself, node);
        node.put(Command.PING);
        send(r, node.address(), node.port());
    }

    public void sendSampleInfohashes(ByteBuffer myself, ByteBuffer range, Node node)
            throws Exception {
        SampleInfoHashesRequest r = new SampleInfoHashesRequest(myself, range, node);
        node.put(Command.SAMPLE);
        send(r, node.address(), node.port());
    }

    public void tick() {
        double deltaSeconds = TICK_INTERVAL_MS / 1000.0;
        List<UTPManager.PendingPacket> pending = utpManager.tick(deltaSeconds);
        for (UTPManager.PendingPacket pp : pending) {
            try {
                InetAddress tickAddr = InetAddress.getByName(pp.ip());
                writeUdp(pp.data(), tickAddr, pp.port());
            } catch (Exception ex) {
                logger.error("Failed to send UTP tick packet: {}", ex.getMessage(), ex);
            }
        }
    }

    private void writeUdp(byte[] data, InetAddress addr, int port) {
        if (this.channel == null || !this.channel.isActive()) {
            // Channel is gone (remote side dropped the connection). Do NOT allocate
            // a ByteBuf that nobody will release — just drop the packet with a warning.
            logger.warn("writeUdp: channel not active, dropping {} byte(s) to {}:{}", data.length, addr, port);
            return;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        DatagramPacket pkt = new DatagramPacket(buf, new InetSocketAddress(addr, port));
        this.channel.writeAndFlush(pkt).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                // Netty releases the DatagramPacket (and its ByteBuf) through the pipeline
                // on failure, but if the channel closed between the isActive() guard and
                // the actual write, Netty may not have taken ownership. Defend with
                // safeRelease so we never double-release.
                logger.warn("writeUdp: failed to send {} byte(s) to {}:{}: {}", data.length, addr, port, f.cause().getMessage());
                if (pkt.refCnt() > 0) {
                    ReferenceCountUtil.safeRelease(pkt);
                }
            }
        });
    }
}
