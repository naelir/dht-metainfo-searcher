package com.naeir.bt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.naelir.dht.From;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    public static final Logger logger = LogManager.getLogger(ClientHandler.class);
    public static final Path CACHE_FILE = Paths.get(System.getProperty("user.home"), "torrents.info");
    private ByteBuffer myself;
    private Torrent torrent;

    public ClientHandler(ByteBuffer myself, Torrent torrent) {
        this.myself = myself;
        this.torrent = torrent;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channel.writeAndFlush(new HandshakeRequest(this.torrent.infoHash.array(), this.myself.array()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.info("channel {} closed", ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("received {}", msg.getClass().getSimpleName());
        Channel channel = ctx.channel();
        if (msg instanceof HandshakeRequest) {
            channel.writeAndFlush(new ExtendedHandshakeRequest(1));
        } else if (msg instanceof ExtendedHandshakeRequest ehr) {
            channel.writeAndFlush(new UtMetaRequest(ehr.utCode));
        } else if (msg instanceof RawTorrentMetadata data) {
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            From from = new From(addr.getAddress().getAddress(), addr.getPort());
            Optional<TorrentMeta> torrentMeta = data.resolve(from);
            if (torrentMeta.isPresent()) {
                TorrentMeta meta = torrentMeta.get();
                if (this.torrent != null) {
                    this.torrent.setMeta(meta);
                }
                logger.info(meta);
                save(meta);
            }
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error(cause);
        ctx.close();
    }

    synchronized void save(TorrentMeta meta) {
        try (
                BufferedWriter bufferedWriter = Files.newBufferedWriter(CACHE_FILE, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            ObjectMapper me = new ObjectMapper();
            ObjectWriter writer = me.writer(new MinimalPrettyPrinter());
            String ms = writer.writeValueAsString(meta);
            bufferedWriter.append(ms);
            bufferedWriter.newLine();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
