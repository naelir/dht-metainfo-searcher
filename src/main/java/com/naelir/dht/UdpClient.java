package com.naelir.dht;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.messages.HandshakeMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class UdpClient implements Runnable, AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 17;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final Logger logger = LogManager.getLogger(UdpClient.class);
    final DatagramSocket socket;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ByteBuffer myself;
    private OnDataListener listener;
    private Data db;

    public UdpClient(Data data) throws Exception {
        this.myself = data.myself;
        this.listener = new OnDataListener(data);
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.socket = new DatagramSocket();
        // A positive timeout lets the receiver thread wake up periodically
        // to check the running flag instead of blocking forever.
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

    private List<Node> contactPoints() throws UnknownHostException {
//        byte[] byName1 = InetAddress.getByName("router.bittorrent.com").getAddress();
        byte[] byName2 = InetAddress.getByName("dht.transmissionbt.com").getAddress();
//        byte[] byName3 = InetAddress.getByName("router.utorrent.com").getAddress();
        return List.of(/* new Node(byName1, 6881), */new Node(byName2, 6881)/* , new Node(byName3, 6881) */);
    }

    public void explore(List<Node> closest) throws Exception {
        if (closest.isEmpty() == false) {
            int fails = 0;
            for (Node node : closest) {
                try {
                    sendFindNode(this.myself, node);
                } catch (Exception e) {
                    fails++;
                    logger.error(e.getMessage());
                }
            }
            if (fails == closest.size()) {
                List<Node> contactPoints = contactPoints();
                for (Node node : contactPoints) {
                    sendFindNode(this.myself, node);
                }
            }
        } else {
            List<Node> contactPoints = contactPoints();
            for (Node node : contactPoints) {
                sendFindNode(this.myself, node);
            }
        }
    }

    private boolean isEmpty(byte[] addr) {
        for (byte element : addr) {
            if (element != 0)
                return false;
        }
        return true;
    }

    private void logTo(Object decode, From from) {
        logger.debug("{}, {} to {}, port {}", decode.getClass().getSimpleName(), decode, Generator.inet(from.ip),
                from.port);
    }

    @Override
    public void run() {
        this.running.set(true);
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
        }
    }

    public void send(byte[] encode, InetAddress addr, int port) throws Exception {
        From from = new From(addr.getAddress(), port);
        if (isEmpty(from.ip) == false) {
            DatagramPacket packet = new DatagramPacket(encode, encode.length, addr, port);
            this.socket.send(packet);
        }
    }

    public void send(IRequest request, InetAddress addr, int port) throws Exception {
        From from = new From(addr.getAddress(), port);
        logTo(request, from);
        byte[] encode = BEncoder.encode(request);
        if (isEmpty(from.ip) == false && port > 0) {
            this.db.sent.put(request.tid(), request);
            DatagramPacket packet = new DatagramPacket(encode, encode.length, addr, port);
            this.socket.send(packet);
        }
    }

    public void sendAnnouncePeer(ByteBuffer torrent, Node node) throws Exception {
        Token token = new Token(node.ip);
        AnnouncePeerRequest r = new AnnouncePeerRequest(this.myself, torrent, token.value, this.socket.getPort(), node);
        node.put(Command.ANNOUNCE);
        send(r, node.address(), node.port);
    }

    public void sendFindNode(ByteBuffer id, Node node) throws Exception {
        FindNodeRequest r = new FindNodeRequest(this.myself, id, node);
        node.put(Command.FIND_NODE);
        send(r, node.address(), node.port);
    }

    public void sendGetPeers(ByteBuffer torrent, Node node) throws Exception {
        GetPeersRequest r = new GetPeersRequest(this.myself, torrent, node);
        node.put(Command.GET_PEER);
        send(r, node.address(), node.port);
    }

    public void sendHandshake(Node node) throws Exception {
        HandshakeMessage name = new HandshakeMessage();
        ByteBuf buffer = Unpooled.buffer();
        name.write(buffer);
        send(buffer.array(), node.address(), node.port);
    }

    public void sendPing(Node node) throws Exception {
        PingRequest r = new PingRequest(this.myself, node);
        node.put(Command.PING);
        send(r, node.address(), node.port);
    }

    public void sendSampleInfohashes(ByteBuffer range, Node node) throws UnknownHostException, Exception {
        SampleInfoHashesRequest r = new SampleInfoHashesRequest(this.myself, range, node);
        node.put(Command.SAMPLE);
        send(r, node.address(), node.port);
    }

    public void start() {
        new Thread(this, "udp").start();
    }

    void stop() {
        this.running.set(false);
    }
}