package com.naelir.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

public class Generator {
    // imitate something
    private static final String NAME = "-SZ1000-";
    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] client(byte[] peerId) {
        return Arrays.copyOfRange(peerId, 0, 8);
    }

    public static int client(Object o) {
        if (o instanceof IRequest ir)
            return ir.id().getInt(0);
        else if (o instanceof IResponse irsp)
            return irsp.id().getInt(0);
        return 0;
    }

    public static String clientPrefix(byte[] peerId) {
        return new String(client(peerId), StandardCharsets.UTF_8);
    }

    public static String generatePeerID() {
        String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(12);
        return NAME.concat(randomAlphanumeric);
    }

    public static byte[] generateRandomByteID() {
        byte[] nid = new byte[20];
        new Random().nextBytes(nid);
        return nid;
    }

    public static ByteBuffer generateRandomID() {
        return ByteBuffer.wrap(generateRandomByteID());
    }

    public static InetAddress inet(byte[] ip) {
        try {
            return InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static String ip(byte[] ip) {
        return String.format("%d.%d.%d.%d", Byte.toUnsignedInt(ip[0]), Byte.toUnsignedInt(ip[1]),
                Byte.toUnsignedInt(ip[1]), Byte.toUnsignedInt(ip[1]));
    }

    public static byte[] sha1(byte[] ip) {
        try {
            byte[] token = new byte[8];
            byte[] digest = MessageDigest.getInstance("SHA1").digest(ip);
            System.arraycopy(digest, 0, token, 0, 8);
            return token;
        } catch (NoSuchAlgorithmException ex) {
            return new byte[0];
        }
    }

    public static byte[] toArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
//
//    static ByteBuffer getTid(int id) {
//        id = id % Config.MAX_MESSAGE_ID;
//        byte[] bytes = { (byte) (id & 0xFF), (byte) ((id & 0xFF00) >>> 8) };
//        return ByteBuffer.wrap(bytes);
//    }

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = Generator.HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = Generator.HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static List<String> toHex(List<Node> nodes) {
        return nodes.stream().map(e -> toHex(e.id.array())).toList();
    }
}
