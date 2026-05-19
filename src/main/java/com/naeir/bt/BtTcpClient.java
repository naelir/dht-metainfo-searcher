package com.naeir.bt;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;

import com.naelir.dht.Generator;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class BtTcpClient {
    public static void main(String[] args) throws Exception {
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        ByteBuffer myself = Generator.generateRandomID();
        ByteBuffer torrent = ByteBuffer.wrap("fa364df414550722d55d8f03071faa5fb782a3af".getBytes());
        ChannelFuture future = new BtTcpClient(torrent, myself, Collections.emptyMap()).connect(addr, 6881);
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private ByteBuffer hash;
    private ByteBuffer myself;
    private Map<ByteBuffer, Torrent> torrents;

    public BtTcpClient(ByteBuffer hash, ByteBuffer myself, Map<ByteBuffer, Torrent> torrents) {
        this.hash = hash;
        this.myself = myself;
        this.torrents = torrents;
    }

    public ChannelFuture connect(InetAddress addr, int port) {
        IoHandlerFactory newFactory = NioIoHandler.newFactory();
        MultiThreadIoEventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(newFactory);
        return new Bootstrap().group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, 4096)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("he", new HandshakeEncoder());
                        pipeline.addLast("hd", new HandshakeDecoder(BtTcpClient.this.hash));
                        pipeline.addLast("ch", new ClientHandler(BtTcpClient.this.hash, BtTcpClient.this.myself,
                                BtTcpClient.this.torrents));
                    }
                })
                .connect(addr, port);
    }
}