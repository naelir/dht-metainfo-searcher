package com.naelir.bt.messages;

public class UnchokeMessage extends AbstractPeerWireMessage {
    public UnchokeMessage() {
        super(BtKeys.UNCHOKE_MESSAGE_ID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "UNCHOKE []";
    }
}
