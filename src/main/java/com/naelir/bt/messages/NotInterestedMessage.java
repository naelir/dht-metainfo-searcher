package com.naelir.bt.messages;

public class NotInterestedMessage extends AbstractPeerWireMessage {
    public NotInterestedMessage() {
        super(BtKeys.NOTINT_MESSAGE_ID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NOT_INTERESTED []";
    }
}
