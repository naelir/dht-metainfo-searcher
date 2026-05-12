package com.naeir.bt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HandshakeEncoder extends MessageToByteEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof HandshakeRequest hm) {
            hm.write(out);
        } else if (msg instanceof ExtendedHandshakeRequest eh) {
            eh.write(out);
        } else if (msg instanceof UtMetaRequest br) {
            br.write(out);
        }
    }
}