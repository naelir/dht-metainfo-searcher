package com.naelir.bt.messages;

public class InterestedMessage extends AbstractPeerWireMessage {
    public InterestedMessage() {
        super(BtKeys.INT_MESSAGE_ID);
    }

    @Override
    public String toString() {
        return "INTERESTED []";
    }
}
