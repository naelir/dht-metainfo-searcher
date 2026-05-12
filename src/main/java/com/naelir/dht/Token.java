/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Token {
    private static final Random RANDOM = new Random();

    public static ByteBuffer token(byte[] ip) {
        try {
            MessageDigest SHA1 = MessageDigest.getInstance("SHA1");
            byte[] digest = SHA1.digest(ip);
            return ByteBuffer.wrap(digest);
        } catch (NoSuchAlgorithmException e) {
            byte[] tokenBA = new byte[Config.TOKEN_LENGTH];
            RANDOM.nextBytes(tokenBA);
            return ByteBuffer.wrap(tokenBA);
        }
    }

    ByteBuffer value;
//    private ByteBuffer nodeID;
    private long generatedTime;

    public Token(byte[] ip) {
        this.value = token(ip);
//        this.nodeID = nodeId;
        this.generatedTime = System.currentTimeMillis();
    }
//
//    public Token(ByteBuffer nodeId, ByteBuffer token) {
//        this.token = token;
////        this.nodeID = nodeId;
//        this.generatedTime = System.currentTimeMillis();
//    }

    public long generated() {
        return this.generatedTime;
    }

    public ByteBuffer value() {
        return this.value;
    }
}
