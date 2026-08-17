package com.naelir.bt.messages;

public class UnchokeMessage extends AbstractPeerWireMessage {
    public UnchokeMessage() {
        super(BtKeys.UNCHOKE_MESSAGE_ID);
    }

    @Override
    public String toString() {
        return "UNCHOKE []";
    }
}
