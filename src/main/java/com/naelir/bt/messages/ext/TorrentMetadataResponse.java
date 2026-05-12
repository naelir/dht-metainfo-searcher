package com.naelir.bt.messages.ext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TorrentMetadataResponse extends ExtendedPeerWireMessage {
    public static final int METADATA_PIECE_SIZE = 16 << 10;
    public static final Logger logger = LogManager.getLogger(TorrentMetadataResponse.class);
    byte[] all;
    public UtMetadataResponse msg;
    public byte[] meta;

    public TorrentMetadataResponse(byte code) {
        super(code);
    }

    @Override
    public byte[] bencode() {
        return new byte[0];
    }

    @Override
    public void read(byte[] bytes) {
        this.msg = new UtMetadataResponse(0, this.extensionId);
        this.msg.read(bytes);
        int remain = bytes.length - this.msg.size;
        this.meta = new byte[remain];
        System.arraycopy(bytes, this.msg.size, this.meta, 0, remain);
    }

    @Override
    public String toString() {
        return "TorrentMetadataResponse []";
    }
}