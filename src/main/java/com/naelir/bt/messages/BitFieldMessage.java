package com.naelir.bt.messages;

import java.util.BitSet;

import io.netty.buffer.ByteBuf;

public class BitFieldMessage extends AbstractPeerWireMessage {
    public BitSet bitSet;
    private int length;

    public BitFieldMessage(BitSet bitSet) {
        super(BtKeys.BIT_MESSAGE_ID);
        this.bitSet = bitSet;
    }

    public BitFieldMessage(int length) {
        super(BtKeys.BIT_MESSAGE_ID);
        this.length = length;
    }

    @Override
    public void readImpl(ByteBuf buffer) {
        byte[] name = new byte[this.length];
        buffer.readBytes(name);
        this.bitSet = BitSet.valueOf(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BITFIELD";
    }

    @Override
    public void writeImpl(ByteBuf buffer) {
        for (int i = 0; i < this.bitSet.size();) {
            byte data = 0;
            for (int j = 128; i < this.bitSet.size() && j > 0; j >>= 1, i++) {
                if (this.bitSet.get(i)) {
                    data |= j;
                }
            }
            buffer.writeByte(data);
        }
    }
}
