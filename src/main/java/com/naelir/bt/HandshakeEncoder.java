package com.naelir.bt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.messages.AbstractPeerWireMessage;
import com.naelir.bt.messages.HandshakeMessage;
import com.naelir.bt.messages.ext.ExtendedMessageHandshake;
import com.naelir.bt.messages.ext.ExtendedPeerWireMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HandshakeEncoder extends MessageToByteEncoder<Object> {
    public static final Logger logger = LogManager.getLogger(HandshakeEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        logger.debug("sending {}", msg.getClass().getSimpleName());
        if (msg instanceof HandshakeMessage hm) {
            hm.write(out);
        } else if (msg instanceof ExtendedPeerWireMessage br) {
            br.write(out);
        } else if (msg instanceof ExtendedMessageHandshake eh) {
            eh.write(out);
        } else if (msg instanceof AbstractPeerWireMessage apwm) {
            out.readerIndex(0);
            out.writeInt(0x00);
            apwm.write(out);
            out.readerIndex(1);
            final int len = out.readableBytes();
            out.setInt(0, len);
        }
    }
}