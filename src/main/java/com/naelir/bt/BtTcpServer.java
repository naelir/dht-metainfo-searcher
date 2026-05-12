package com.naelir.bt;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class BtTcpServer implements AutoCloseable {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    private int port;
    private NioServerSocketChannel serverChannel;

    public BtTcpServer(int port) {
        this.port = port;
    }

    @Override
    public void close() throws Exception {
        this.serverChannel.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
    }

    public void start() {
        try {
            this.serverBootstrap.group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .localAddress(this.port)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            ctx.channel().disconnect();
                        }
                    })
                    .validate();
            this.serverChannel = (NioServerSocketChannel) this.serverBootstrap
                    .bind(new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), this.port))
                    .channel();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
