/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Random;

public class Token {
    private static final Random RANDOM = new Random();

    private static ByteBuffer newToken(byte[] ip) {
        byte[] tokenBA = new byte[Config.TOKEN_LENGTH];
        RANDOM.nextBytes(tokenBA);
        return ByteBuffer.wrap(tokenBA);
    }

    ByteBuffer token;
    private ByteBuffer nodeID;
    private long generatedTime;

    public Token(ByteBuffer id, byte[] ip) {
        this.token = newToken(ip);
        this.nodeID = id;
        this.generatedTime = System.currentTimeMillis();
    }

    public long generated() {
        return this.generatedTime;
    }

    public ByteBuffer nodeId() {
        return this.nodeID;
    }

    public ByteBuffer token() {
        return this.token;
    }
}
