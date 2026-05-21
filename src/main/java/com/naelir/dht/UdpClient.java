package com.naelir.dht;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Node.Command;

public class UdpClient implements Runnable, AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final Logger logger = LogManager.getLogger(UdpClient.class);

    static ByteBuffer getTid(int id) {
        id = id % Config.MAX_MESSAGE_ID;
        byte[] bytes = { (byte) (id & 0xFF), (byte) ((id & 0xFF00) >>> 8) };
        return ByteBuffer.wrap(bytes);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

//    ExecutorService executor = Executors.newSingleThreadExecutor();
    final DatagramSocket socket;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ByteBuffer myself;
    private IOnDataListener listener;
    private Data db;

    public UdpClient(Data data, IOnDataListener listener) throws Exception {
        this.myself = data.myself;
        this.listener = listener;
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
        byte[] byName1 = InetAddress.getByName("router.bittorrent.com").getAddress();
        byte[] byName2 = InetAddress.getByName("dht.transmissionbt.com").getAddress();
        byte[] byName3 = InetAddress.getByName("router.utorrent.com").getAddress();
        return List.of(new Node(byName1, 6881, null), new Node(byName2, 6881, null), new Node(byName3, 6881, null));
    }

    public void explore() throws Exception {
        List<Node> contactPoints = contactPoints();
        ByteBuffer tid = getTid(1);
        for (Node node : contactPoints) {
            FindNodeRequest initial = new FindNodeRequest(tid, this.myself, this.myself);
            send(initial, node.address(), node.port);
        }
    }

    private void logTo(Object decode, From from) {
        logger.info("{}, to {}, port {}", decode.getClass().getSimpleName(), Arrays.toString(from.ip), from.port);
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

    public void send(IRequest request, InetAddress addr, int port) throws Exception {
        logTo(request, new From(addr.getAddress(), port));
        byte[] encode = BEncoder.encode(request);
        this.db.sent.put(new CommandId(request.tid(), addr.getAddress(), port), request);
        DatagramPacket packet = new DatagramPacket(encode, encode.length, addr, port);
        this.socket.send(packet);
    }

    public void sendAnnouncePeer(ByteBuffer torrent, Node node) throws Exception {
        ByteBuffer nextId = getTid(node.nextId());
        Token token = new Token(node.ip);
        AnnouncePeerRequest r = new AnnouncePeerRequest(nextId, this.myself, torrent, token.value,
                this.socket.getPort());
        node.command(Command.ANNOUNCE);
        send(r, node.address(), node.port);
    }

    public void sendFindNode(ByteBuffer id, Node node) throws Exception {
        ByteBuffer nextId = getTid(node.nextId());
        FindNodeRequest r = new FindNodeRequest(nextId, this.myself, id);
        node.command(Command.FIND_NODE);
        send(r, node.address(), node.port);
    }

    public void sendGetPeers(ByteBuffer torrent, Node node) throws Exception {
        ByteBuffer nextId = getTid(node.nextId());
        GetPeersRequest r = new GetPeersRequest(nextId, this.myself, torrent);
        node.command(Command.GET_PEER);
        send(r, node.address(), node.port);
    }

    public void sendPing(Node node) throws Exception {
        ByteBuffer nextId = getTid(node.nextId());
        PingRequest r = new PingRequest(nextId, this.myself);
        node.command(Command.PING);
        send(r, node.address(), node.port);
    }

    public void sendSampleInfohashes(ByteBuffer range, Node node) throws UnknownHostException, Exception {
        ByteBuffer nextId = getTid(node.nextId());
        SampleInfoHashesRequest r = new SampleInfoHashesRequest(nextId, this.myself, range);
        node.query(Command.SAMPLE);
        send(r, node.address(), node.port);
    }

    public void start() {
        new Thread(this, "udp").start();
//        this.executor.execute(this);
    }

    void stop() {
        this.running.set(false);
//        this.executor.shutdown();
    }
}