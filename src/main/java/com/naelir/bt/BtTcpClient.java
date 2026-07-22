package com.naelir.bt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.Node;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

public class BtTcpClient {
    public static final Logger logger = LogManager.getLogger(BtTcpClient.class);
    private Data data;
    private Torrent torrent;
    private Node node;

    public BtTcpClient(Torrent torrent, Node node, Data data) {
        this.torrent = torrent;
        this.node = node;
        this.data = data;
    }

    public void connect() throws InterruptedException, UnknownHostException {
        var group = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("bt"), NioIoHandler.newFactory());
        try {
            Bootstrap bootstrap = new Bootstrap();
            ChannelFuture connectFuture = bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.SO_RCVBUF, 4096)
                    .option(ChannelOption.SO_SNDBUF, 4096)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .handler(new ChannelHandler(this.torrent, this.data))
                    .connect(this.node.address(), this.node.port());
            // awaitUninterruptibly avoids spurious wakeups breaking the connect wait
            connectFuture.awaitUninterruptibly();

            InetAddress address = this.node.address();
            String country = IpRangeFilter.getCountry(address.getAddress());
            if (!connectFuture.isSuccess()) {
                // connection refused, timed-out, etc. — no channel to close
                logger.warn("Connection to {} {}:{} failed: {}", country, this.node.address(), this.node.port(),
                        connectFuture.cause().getMessage());
                return;
            } else {
                logger.warn("Connection to {} {}:{} succeeded", country, this.node.address(), this.node.port());
            }
            // blocks until channel is closed: either RawTorrentMetadata received,
            // error in ClientHandler, or IdleStateHandler fires after 1s of silence
            connectFuture.channel().closeFuture().sync();
        } finally {
            // default shutdownGracefully() quiet-period=2s / timeout=15s — far too long
            // when connect() is called in a tight loop for many peers
            group.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS).sync();
        }
    }

    private static final class ChannelHandler extends ChannelInitializer<SocketChannel> {
        private Torrent task;
        private Data data;

        public ChannelHandler(Torrent task, Data data) {
            this.task = task;
            this.data = data;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("idle", new IdleStateHandler(5, 5, 0));
            pipeline.addLast("ich", new IdleChannelHandler());
            pipeline.addLast("he", new HandshakeEncoder());
            pipeline.addLast("hd", new HandshakeDecoder());
            pipeline.addLast("ch", new ClientHandler(this.data, this.task));
        }
    }

    static class IdleChannelHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                Channel idleChannel = ctx.channel();
                idleChannel.close().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        logger.debug("disconnecting connection: {}", future.channel());
                    }
                });
            }
        }
    }
}