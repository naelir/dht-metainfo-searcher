package com.naeir.bt;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import com.naelir.dht.From;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ByteBuffer torrent;
    private ByteBuffer myself;
    private Map<ByteBuffer, Torrent> torrents;

    public ClientHandler(ByteBuffer torrent, ByteBuffer myself, Map<ByteBuffer, Torrent> torrents) {
        this.torrent = torrent;
        this.myself = myself;
        this.torrents = torrents;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channel.writeAndFlush(new HandshakeRequest(this.torrent.array(), this.myself.array()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(msg);
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
                Torrent found = this.torrents.get(this.torrent);
                if (found != null) {
                    found.setMeta(torrentMeta.get());
                }
                System.out.println(torrentMeta);
            }
        }
    }
}
