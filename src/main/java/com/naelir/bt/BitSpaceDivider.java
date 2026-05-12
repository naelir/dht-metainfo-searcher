package com.naelir.bt;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BitSpaceDivider {
    /**
     * Generates {@code n} (even) keys that equally divide the 160-bit Kademlia key
     * space.
     * <p>
     * The keys are arithmetic multiples of the step {@code 2^160 / n}, which
     * corresponds to equal XOR-distance between every neighbouring key in the
     * returned list when the keys are laid out on the circular 160-bit number line.
     * Each key is returned as a 20-byte (160-bit) big-endian {@link ByteBuffer}.
     *
     * @param n an even number of dividers to generate
     * @return list of {@code n} {@link ByteBuffer}s, each holding a 20-byte key
     * @throws IllegalArgumentException if {@code n} is not positive or not even
     */
    public static List<ByteBuffer> divide(int n) {
        if (n <= 0 || n % 2 != 0)
            throw new IllegalArgumentException("n must be a positive even number, got: " + n);
        // 2^160 — the size of the full Kademlia key space
        BigInteger keySpace = BigInteger.ONE.shiftLeft(160);
        // Arithmetic step between consecutive keys; equal XOR-distance on the number
        // line
        BigInteger step = keySpace.divide(BigInteger.valueOf(n));
        List<ByteBuffer> keys = new ArrayList<>(n);
        BigInteger current = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            keys.add(ByteBuffer.wrap(to160BitBytes(current)));
            current = current.add(step);
        }
        return keys;
    }

    /**
     * Converts a non-negative {@link BigInteger} to a fixed-length 20-byte
     * (160-bit) big-endian byte array, padding with leading zeroes as necessary.
     */
    private static byte[] to160BitBytes(BigInteger value) {
        byte[] raw = value.toByteArray(); // may have a leading 0x00 sign byte
        byte[] out = new byte[20];
        if (raw.length <= 20) {
            // right-align into the 20-byte array (big-endian, zero-padded on the left)
            System.arraycopy(raw, 0, out, 20 - raw.length, raw.length);
        } else {
            // strip the leading sign byte (raw.length == 21 with a 0x00 prefix)
            System.arraycopy(raw, raw.length - 20, out, 0, 20);
        }
        return out;
    }
}