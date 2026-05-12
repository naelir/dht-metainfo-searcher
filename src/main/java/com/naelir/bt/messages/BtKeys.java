package com.naelir.bt.messages;

public final class BtKeys {
    public static final String METADATA_SIZE = "metadata_size";
    public static final String M = "m";
    public static final String UT_METADATA = "ut_metadata";
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String LENGTH = "length";
    public static final String FILES = "files";
    public static final String PIECE = "piece";
    public static final String MSG_TYPE = "msg_type";
    public static final String METHOD = "ut_metadata";
    public static final int CHOKE_MESSAGE_ID = 0x00;
    public static final int UNCHOKE_MESSAGE_ID = 0x01;
    public static final int INT_MESSAGE_ID = 0x02;
    public static final int NOTINT_MESSAGE_ID = 0x03;
    public static final int HAVE_MESSAGE_ID = 0x04;
    public static final int BIT_MESSAGE_ID = 0x05;
    public static final int REQ_MESSAGE_ID = 0x06;
    public static final int BLOCK_MESSAGE_ID = 0x07;
    public static final int CANCEL_MESSAGE_ID = 0x08;
    public static final int PORT_MESSAGE_ID = 0x09;
    public static final int HAVE_NONE_ID = 0x0f;
    public static final int EXTENDED_MESSAGE_ID = 20;
    
    private BtKeys() {
    }
}
