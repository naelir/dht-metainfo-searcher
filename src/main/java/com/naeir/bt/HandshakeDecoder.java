package com.naeir.bt;

import java.nio.ByteBuffer;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class HandshakeDecoder extends ByteToMessageDecoder {
    private boolean handshaken;
    private boolean ext;
    private RawTorrentMetadata data;

    public HandshakeDecoder(ByteBuffer torrent) {
        this.handshaken = false;
        this.ext = false;
        this.data = new RawTorrentMetadata(torrent);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes < 2)
            return;
        if (this.handshaken == false) {
            HandshakeRequest data = new HandshakeRequest();
            data.read(in);
            out.add(data);
            this.handshaken = true;
        } else if (this.ext == false) {
            ExtendedHandshakeRequest name = new ExtendedHandshakeRequest();
            name.read(in);
            out.add(name);
            this.ext = true;
        } else if (this.data.isComplete() == false) {
            this.data.read(in);
            if (this.data.isComplete()) {
                out.add(this.data);
            }
        }
    }
}