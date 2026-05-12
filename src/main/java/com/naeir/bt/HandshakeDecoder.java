package com.naeir.bt;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class HandshakeDecoder extends ByteToMessageDecoder {
    private boolean handshaken;
    private boolean ext;
    private RawTorrentMetadata data;

    public HandshakeDecoder() {
        this.handshaken = false;
        this.ext = false;
        this.data = new RawTorrentMetadata();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
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
        } else {
            this.data.read(in);
            if (this.data.bytes.position() == this.data.bytes.capacity()) {
                out.add(this.data);
            }
        }
    }
}