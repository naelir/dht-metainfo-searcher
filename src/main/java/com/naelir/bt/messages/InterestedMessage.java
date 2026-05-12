package com.naelir.bt.messages;

public class InterestedMessage extends AbstractPeerWireMessage {
    public InterestedMessage() {
        super(BtKeys.INT_MESSAGE_ID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "INTERESTED []";
    }
}
