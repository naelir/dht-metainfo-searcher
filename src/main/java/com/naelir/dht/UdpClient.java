package com.naelir.dht;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;

public class UdpClient implements Runnable, AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final Logger logger = LogManager.getLogger(UdpClient.class);

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9876;
        try (UdpClient client = new UdpClient()) {
            // The caller creates and owns the receiver thread.
            Thread receiverThread = new Thread(client, "udp-receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();
        }
    }

    private final DatagramSocket socket;
    private final int bufferSize;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ResponseResolver resolver;

    public UdpClient() throws Exception {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.socket = new DatagramSocket();
        // A positive timeout lets the receiver thread wake up periodically
        // to check the running flag instead of blocking forever.
        this.socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
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
        while (this.running.get()) {
            try {
                byte[] buffer = new byte[this.bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                byte[] address = packet.getAddress().getAddress();
                From from = new From(address, packet.getPort());
                Optional<BencodedDictionary> decode = BDecoder.decode(data);
                if (decode.isPresent()) {
                    BencodedDictionary bmap = decode.get();
                    Optional<byte[]> response = this.resolver.resolve(bmap, from);
                    if (response.isPresent()) {
                        byte[] encode = response.get();
                        this.socket.send(new DatagramPacket(encode, encode.length));
                    }
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

    public void send(byte[] data, InetAddress serverAddress, int serverPort) throws Exception {
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        this.socket.send(packet);
    }

    void stop() {
        this.running.set(false);
    }
}