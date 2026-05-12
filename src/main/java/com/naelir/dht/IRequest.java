package com.naelir.dht;

import java.nio.ByteBuffer;

public interface IRequest {
    String method();

    ByteBuffer tid();
}
