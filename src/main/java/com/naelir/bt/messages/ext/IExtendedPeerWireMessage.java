package com.naelir.bt.messages.ext;

import io.netty.buffer.ByteBuf;

public interface IExtendedPeerWireMessage {
    void write(ByteBuf buffer);

    void read(byte[] buffer);
}
