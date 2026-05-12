package com.naelir.bt.messages;

import io.netty.buffer.ByteBuf;

public interface PeerWireMessage {
	void write(ByteBuf buffer);

	void read(ByteBuf buffer);
}
