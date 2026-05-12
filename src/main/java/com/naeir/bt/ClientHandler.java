package com.naeir.bt;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

import com.naelir.dht.From;
import com.naelir.dht.Generator;
import com.naelir.dht.UdpClient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private String torrent;

    public ClientHandler(String torrent) {
        this.torrent = torrent;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        byte[] hash = UdpClient.hexStringToByteArray(this.torrent);
        ByteBuffer randomID = Generator.generateRandomID();
        Channel channel = ctx.channel();
        channel.writeAndFlush(new HandshakeRequest(hash, randomID.array()));
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
            Optional<TorrentMeta> torrentMeta = data.resolve(this.torrent, from);
            if (torrentMeta.isPresent()) {
                System.out.println(torrentMeta);
            }
        }
    }
}
