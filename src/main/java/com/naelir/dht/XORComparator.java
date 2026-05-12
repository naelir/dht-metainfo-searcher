package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class XORComparator implements Comparator<Node> {
    private final byte[] target;

    public XORComparator(ByteBuffer target) {
        this.target = target.array();
    }

    @Override
    public int compare(Node a, Node b) {
        byte a_ba[] = a.id.array();
        byte b_ba[] = b.id.array();
        for (int i = 0; i < this.target.length; i++) {
            int ld = (a_ba[i] ^ this.target[i]) & 0xff;
            int rd = (b_ba[i] ^ this.target[i]) & 0xff;
            if (ld < rd)
                return -1;
            if (ld > rd)
                return 1;
        }
        return 0;
    }
}