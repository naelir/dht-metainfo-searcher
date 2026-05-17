package com.naeir.bt;

import java.net.InetAddress;
import java.nio.ByteBuffer;
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
        ChannelFuture future = new BtTcpClient("fa364df414550722d55d8f03071faa5fb782a3af", myself).connect(addr, 6881);
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private String hash;
    private ByteBuffer myself;

    public BtTcpClient(String hash, ByteBuffer myself) {
        this.hash = hash;
        this.myself = myself;
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
                        pipeline.addLast("hd", new HandshakeDecoder());
                        pipeline.addLast("ch", new ClientHandler(BtTcpClient.this.hash, BtTcpClient.this.myself));
                    }
                })
                .connect(addr, port);
    }
}