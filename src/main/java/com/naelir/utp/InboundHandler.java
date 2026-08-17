package com.naelir.utp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import com.naelir.dht.UdpOnDataListener;
import com.naelir.tracker.TrackerOnDataListener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

/**
 * Receives incoming {@link DatagramPacket}s from the Netty pipeline, delegates
 * to {@link UtpOnDataListener#onData} when the payload is a valid uTP datagram,
 * and writes any response back to the sender.
 */
public class InboundHandler extends ChannelInboundHandlerAdapter {

    private UtpOnDataListener utp;
    private UdpOnDataListener udp;
    private TrackerOnDataListener tracker;
    private final AtomicLong packetCount = new AtomicLong();

    public InboundHandler(UtpOnDataListener utp, UdpOnDataListener udp, TrackerOnDataListener tracker) {
        this.utp = utp;
        this.udp = udp;
        this.tracker = tracker;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket pkt) {
            long count = this.packetCount.incrementAndGet();
            if (count % 1000 == 0) {
                UtpClient.logger.info("Processed {} datagram packets", count);
            }
            try {
                InetSocketAddress sender = pkt.sender();
                InetAddress addr = sender.getAddress();
                int port = sender.getPort();
                int readableBytes = pkt.content().readableBytes();
                byte[] data = new byte[readableBytes];
                pkt.content().readBytes(data);
                if (UtpClient.isUtpPacket(data)) {
                    this.utp.onData(data, addr, port).ifPresent(response -> {
                        ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                        DatagramPacket reply = new DatagramPacket(respBuf, sender);
                        ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                            if (!f.isSuccess()) {
                                UtpClient.logger.warn("channelRead: failed to send uTP reply to {}:{}: {}", addr, port,
                                        f.cause().getMessage());

                                if (reply.refCnt() > 0) {
                                    ReferenceCountUtil.safeRelease(reply);
                                }
                            }
                        });
                    });
                } else if (TrackerOnDataListener.isTrackerPacket(data)) {
                    this.tracker.onData(data, addr, port).ifPresent(response -> {
                        ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                        DatagramPacket reply = new DatagramPacket(respBuf, sender);
                        ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                            if (!f.isSuccess()) {
                                UtpClient.logger.warn("channelRead: failed to send tracker reply to {}:{}: {}", addr, port,
                                        f.cause().getMessage());

                                if (reply.refCnt() > 0) {
                                    ReferenceCountUtil.safeRelease(reply);
                                }                                }
                        });
                    });
                } else {
                    this.udp.onData(data, addr, port).ifPresent(response -> {
                        ByteBuf respBuf = Unpooled.wrappedBuffer(response);
                        DatagramPacket reply = new DatagramPacket(respBuf, sender);
                        ctx.writeAndFlush(reply).addListener((ChannelFuture f) -> {
                            if (!f.isSuccess()) {
                                UtpClient.logger.warn("channelRead: failed to send UDP reply to {}:{}: {}", addr, port,
                                        f.cause().getMessage());

                                if (reply.refCnt() > 0) {
                                    ReferenceCountUtil.safeRelease(reply);
                                }                                }
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
        UtpClient.logger.error("NettyUtpClient inbound error: {}", cause.getMessage());
    }
}