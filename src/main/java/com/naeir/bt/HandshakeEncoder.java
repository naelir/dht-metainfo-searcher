package com.naeir.bt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HandshakeEncoder extends MessageToByteEncoder<Object> {
    public static final Logger logger = LogManager.getLogger(HandshakeEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        logger.info("sending {}", msg.getClass().getSimpleName());
        if (msg instanceof HandshakeRequest hm) {
            hm.write(out);
        } else if (msg instanceof ExtendedHandshakeRequest eh) {
            eh.write(out);
        } else if (msg instanceof UtMetaRequest br) {
            br.write(out);
        }
    }
}