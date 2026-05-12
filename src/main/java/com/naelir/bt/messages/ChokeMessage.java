package com.naelir.bt.messages;

public class ChokeMessage extends AbstractPeerWireMessage {
    public ChokeMessage() {
        super(BtKeys.CHOKE_MESSAGE_ID);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CHOKE []";
    }
}
