package com.naelir.bt;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.messages.BitFieldMessage;
import com.naelir.bt.messages.BlockMessage;
import com.naelir.bt.messages.BtKeys;
import com.naelir.bt.messages.CancelMessage;
import com.naelir.bt.messages.ChokeMessage;
import com.naelir.bt.messages.HandshakeMessage;
import com.naelir.bt.messages.HaveMessage;
import com.naelir.bt.messages.HaveNone;
import com.naelir.bt.messages.InterestedMessage;
import com.naelir.bt.messages.NotInterestedMessage;
import com.naelir.bt.messages.PeerWireMessage;
import com.naelir.bt.messages.PortMessage;
import com.naelir.bt.messages.RequestMessage;
import com.naelir.bt.messages.UnchokeMessage;
import com.naelir.bt.messages.ext.ExtendedMessageHandshake;
import com.naelir.bt.messages.ext.TorrentMetadataResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class HandshakeDecoder extends ReplayingDecoder<com.naelir.bt.HandshakeDecoder.State> {
    public static final Logger logger = LogManager.getLogger(HandshakeDecoder.class);

    public HandshakeDecoder() {
        state(State.HANDSHAKE);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
        case HANDSHAKE: {
            var data = new HandshakeMessage();
            data.read(in);
            out.add(data);
            this.checkpoint(State.NEXT);
            break;
        }
        case NEXT: {
            int length = in.readInt();
            byte type = in.readByte();
            if (type == BtKeys.EXTENDED_MESSAGE_ID) {
                byte messageId = in.readByte();
                byte[] data = new byte[length - 2];
                in.readBytes(data);
                if (messageId > 0) {
                    TorrentMetadataResponse message = new TorrentMetadataResponse(messageId);
                    message.read(data);
                    out.add(message);
                } else {
                    ExtendedMessageHandshake message = new ExtendedMessageHandshake();
                    message.read(data);
                    out.add(message);
                }
            } else {
                PeerWireMessage message = type(type, length - 1);
                message.read(in);
                out.add(message);
            }
            break;
        }
        default:
            break;
        }
    }

    public PeerWireMessage type(byte type, int length) {
        return switch (type) {
        case BtKeys.BIT_MESSAGE_ID -> new BitFieldMessage(length);
        case BtKeys.BLOCK_MESSAGE_ID -> new BlockMessage();
        case BtKeys.CANCEL_MESSAGE_ID -> new CancelMessage();
        case BtKeys.CHOKE_MESSAGE_ID -> new ChokeMessage();
        case BtKeys.HAVE_MESSAGE_ID -> new HaveMessage();
        case BtKeys.HAVE_NONE_ID -> new HaveNone();
        case BtKeys.INT_MESSAGE_ID -> new InterestedMessage();
        case BtKeys.NOTINT_MESSAGE_ID -> new NotInterestedMessage();
        case BtKeys.PORT_MESSAGE_ID -> new PortMessage();
        case BtKeys.REQ_MESSAGE_ID -> new RequestMessage();
        case BtKeys.UNCHOKE_MESSAGE_ID -> new UnchokeMessage();
        default -> new Unknown(type, length);
        };
    }

    enum State {
        HANDSHAKE, NEXT, META, FINISH
    }
}