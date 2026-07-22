package com.naelir.utp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.naelir.Arguments;
import com.naelir.bt.Torrent;
import com.naelir.bt.messages.HandshakeMessage;
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
import com.naelir.dht.OnDataListener;
import com.naelir.dht.PingRequest;
import com.naelir.dht.SampleInfoHashesRequest;
import com.naelir.dht.Token;
import com.naelir.fs.FileDB;
import com.naelir.tracker.ConnectRequest;
import com.naelir.tracker.TrackerOnDataListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

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
public class NettyUtpClient implements AutoCloseable {
    private static final long TICK_INTERVAL_MS = 500;
    public static final Logger logger = LogManager.getLogger(NettyUtpClient.class);

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

    /**
     * Usage: NettyUtpClient [host] [port]
     * <p>
     * Sends a BitTorrent {@link HandshakeMessage} to the given peer over uTP using
     * Netty's NIO transport. Defaults to {@code localhost:56514}.
     */
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "tracker.opentrackr.org";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1337;
        Configurator.setRootLevel(Level.DEBUG);
        ByteBuffer udpId = Generator.generateRandomID();
        String peerId = Generator.generatePeerID();
        Queue<ByteBuffer> udpIds = new LinkedList<>();
        udpIds.add(udpId);
        Data data = new Data(udpIds, peerId, FileDB.of(), Arguments.parse(args));
        UTPManager manager = new UTPManager();
        UtpDataListener utp = new UtpDataListener(manager);
        OnDataListener udp = new OnDataListener(data);
        try (
                NettyUtpClient client = new NettyUtpClient(utp, udp, data)
        ) {
            client.start();
            // tracker.tryhackx.org
            String infoHash = "fc43a8dbe2c723ffd857d13f4cd513a93f251c2e";
            InetAddress addr = InetAddress.getByName("tracker.opentrackr.org");
            client.connectTracker(new Torrent(infoHash), addr, port);
            new Scanner(System.in).nextLine();
            logger.info("Done.");
        }
    }

    private final UtpDataListener listener;
    private final EventLoopGroup group;
    private volatile Channel channel;
    private OnDataListener udp;
    private Data data;
    private TrackerOnDataListener tracker;

    public NettyUtpClient(UtpDataListener utp, OnDataListener udp, Data data) {
        this(utp, udp, data, new TrackerOnDataListener());
    }

    public NettyUtpClient(UtpDataListener utp, OnDataListener udp, Data data, TrackerOnDataListener tracker) {
        this.listener = utp;
        this.udp = udp;
        this.data = data;
        this.tracker = tracker;
        this.group = new NioEventLoopGroup(1, new DefaultThreadFactory("utp-client"));
    }

    @Override
    public void close() {
        this.group.shutdownGracefully();
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
        UTPManager manager = this.listener.getUtpManager();
        InetSocketAddress remote = new InetSocketAddress(addr, port);
        UtpPeerSession session = new UtpPeerSession(this.data, torrent, remote);
        UTPConnection connection = manager.newConnection(session, ip, port);
        // Send the uTP SYN to start the handshake.
        byte[] syn = connection.connect();
        if (syn != null && syn.length > 0) {
            writeUdp(syn, addr, port);
        }
    }

    public void connectPeer(Torrent torrent, Node node) throws Exception {
        connectPeer(torrent, node.address(), node.port());
    }
    // ── Inbound handler ───────────────────────────────────────────────────────

    public void connectTracker(Torrent torrent, InetAddress addr, int port) throws Exception {
        int tid = new Random().nextInt();
        ConnectRequest connectRequest = new ConnectRequest(tid & 0x7FFFFFFF); // Ensure positive transaction ID
        byte[] encode = connectRequest.encode();
        writeUdp(encode, addr, port);
    }
    // ── uTP packet detection ──────────────────────────────────────────────────

    private List<Node> contactPoints() throws UnknownHostException {
//      byte[] byName1 = InetAddress.getByName("router.bittorrent.com").getAddress();
        byte[] byName2 = InetAddress.getByName("dht.transmissionbt.com").getAddress();
//      byte[] byName3 = InetAddress.getByName("router.utorrent.com").getAddress();
        return List.of(/* new Node(byName1, 6881), */new Node(byName2, 6881)/* , new Node(byName3, 6881) */);
    }
    // ── main (smoke-test) ─────────────────────────────────────────────────────

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
        logger.debug("{}, {} to {}, port {}", decode.getClass().getSimpleName(), decode, Generator.inet(from.ip),
                from.port);
    }

    public void send(IRequest request, InetAddress addr, int port) throws Exception {
        From from = new From(addr.getAddress(), port);
        logTo(request, from);
        byte[] encode = BEncoder.encode(request);
        if (port > 0) {
            this.data.sent.put(request.tid(), request);
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
            throws UnknownHostException, Exception {
        SampleInfoHashesRequest r = new SampleInfoHashesRequest(myself, range, node);
        node.put(Command.SAMPLE);
        send(r, node.address(), node.port());
    }

    /**
     * Binds the UDP channel to an ephemeral local port and starts the UTP tick task
     * on the Netty event loop.
     */
    public void start() throws Exception {
        Bootstrap bootstrap = new Bootstrap().group(this.group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .handler(new InboundHandler());
        // Bind to any available local port
        this.channel = bootstrap.bind(0).sync().channel();
        logger.info("NettyUtpClient bound to {}", this.channel.localAddress());
        // Schedule the recurring UTP retransmit tick inside the event loop
        this.group.scheduleAtFixedRate(this::tick, TICK_INTERVAL_MS, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Drives UTP retransmit timers and forwards any resulting packets onto the
     * wire. Runs periodically on the Netty event loop.
     */
    private void tick() {
        double deltaSeconds = TICK_INTERVAL_MS / 1000.0;
        List<UTPManager.PendingPacket> pending = this.listener.getUtpManager().tick(deltaSeconds);
        for (UTPManager.PendingPacket pp : pending) {
            try {
                InetAddress tickAddr = InetAddress.getByName(pp.ip());
                writeUdp(pp.data(), tickAddr, pp.port());
            } catch (Exception ex) {
                logger.error("Failed to send UTP tick packet: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Wraps raw bytes in a Netty {@link DatagramPacket} and sends them through the
     * Netty channel.
     */
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
                logger.warn("writeUdp: failed to send {} byte(s) to {}:{}: {}", data.length, addr, port,
                        f.cause().getMessage());
                ReferenceCountUtil.safeRelease(pkt);
            }
        });
    }

    /**
     * Receives incoming {@link DatagramPacket}s from the Netty pipeline, delegates
     * to {@link UtpDataListener#onData} when the payload is a valid uTP datagram,
     * and writes any response back to the sender.
     */
    private class InboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof DatagramPacket pkt) {
                try {
                    InetSocketAddress sender = pkt.sender();
                    InetAddress addr = sender.getAddress();
                    int port = sender.getPort();
                    byte[] data = new byte[pkt.content().readableBytes()];
                    pkt.content().readBytes(data);
                    if (isUtpPacket(data)) {
                        NettyUtpClient.this.listener.onData(data, addr, port).ifPresent(response -> {
                            ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                            DatagramPacket reply = new DatagramPacket(respBuf, sender);
                            ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                                if (!f.isSuccess()) {
                                    logger.warn("channelRead: failed to send uTP reply to {}:{}: {}", addr, port,
                                            f.cause().getMessage());
                                    ReferenceCountUtil.safeRelease(reply);
                                }
                            });
                        });
                    } else if (TrackerOnDataListener.isTrackerPacket(data)) {
                        NettyUtpClient.this.tracker.onData(data, addr, port).ifPresent(response -> {
                            ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                            DatagramPacket reply = new DatagramPacket(respBuf, sender);
                            ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                                if (!f.isSuccess()) {
                                    logger.warn("channelRead: failed to send tracker reply to {}:{}: {}", addr, port,
                                            f.cause().getMessage());
                                    ReferenceCountUtil.safeRelease(reply);
                                }
                            });
                        });
                    } else {
                        NettyUtpClient.this.udp.onData(data, addr, port).ifPresent(response -> {
                            ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                            DatagramPacket reply = new DatagramPacket(respBuf, sender);
                            ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                                if (!f.isSuccess()) {
                                    logger.warn("channelRead: failed to send UDP reply to {}:{}: {}", addr, port,
                                            f.cause().getMessage());
                                    ReferenceCountUtil.safeRelease(reply);
                                }
                            });
                        });
                    }
                } finally {
                    pkt.release();
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("NettyUtpClient inbound error: {}", cause.getMessage(), cause);
        }
    }
}
