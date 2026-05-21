package com.naeir.bt;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Scanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import com.naelir.dht.Generator;
import com.naelir.dht.UdpClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class BtTcpClient {
    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.DEBUG);
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        ByteBuffer myself = Generator.generateRandomID();
        byte[] btes = UdpClient.hexStringToByteArray("ED6D8004994E94A22EDA3EA891E869E5785258E4");
        ByteBuffer torrent = ByteBuffer.wrap(btes);
        ChannelFuture future = new BtTcpClient(new Torrent(torrent), myself).connect(addr, 54667);
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private ByteBuffer myself;
    private Torrent torrent;

    public BtTcpClient(Torrent torrent, ByteBuffer myself) {
        this.myself = myself;
        this.torrent = torrent;
    }

    public ChannelFuture connect(InetAddress addr, int port) {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        return new Bootstrap().group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, 4096)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("idle", new IdleStateHandler(0, 0, 10));
                        pipeline.addLast("he", new HandshakeEncoder());
                        pipeline.addLast("hd", new HandshakeDecoder(BtTcpClient.this.torrent.infoHash));
                        pipeline.addLast("ch", new ClientHandler(BtTcpClient.this.myself, BtTcpClient.this.torrent));
                    }
                })
                .connect(addr, port);
    }
}