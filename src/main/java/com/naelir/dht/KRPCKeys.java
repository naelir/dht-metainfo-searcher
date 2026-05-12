package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedInteger;
import com.github.cdefgah.bencoder4j.model.BencodedList;

public final class KRPCKeys {
    public static final String TRANSACTIONID = "t";
    public static final String TYPE = "y";
    public static final String ARGUMENTS = "a";
    public static final String QUERY = "q";
    public static final String RESPONSE = "r";
    public static final String ERROR = "e";
    public static final String ID = "id";
    public static final String TARGET = "target";
    public static final String NODES = "nodes";
    public static final String TOKEN = "token";
    public static final String INFO_HASH = "info_hash";
    public static final String VALUES = "values";
    public static final String PORT = "port";
    public static final String IMPLIED_PORT = "implied_port";
    public static final List<String> BEP05_METHODS = List.of(Commands.PING, Commands.ANNOUNCE_PEER, Commands.FIND_NODE,
            Commands.GET_PEERS);
    public static final List<String> BEP051_METHODS = List.of(Commands.SAMPLE_INFOHASHES);

    public static boolean equals(byte[] bytes, String krpc) {
        return Arrays.equals(bytes, krpc.getBytes());
    }

    public static BencodedDictionary getArgs(BencodedDictionary map) {
        return (BencodedDictionary) map.get(ARGUMENTS);
    }

    public static BencodedList getError(BencodedDictionary map) {
        return (BencodedList) map.get(ERROR);
    }

    public static ByteBuffer getId(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(ID);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static Integer getImpliedPort(BencodedDictionary map) {
        BencodedInteger bo = (BencodedInteger) map.get(IMPLIED_PORT);
        return bo != null ? (int) bo.getValue() : null;
    }

    public static ByteBuffer getInfoHash(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(INFO_HASH);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static Integer getInterval(BencodedDictionary map) {
        BencodedInteger type = (BencodedInteger) map.get("interval");
        return type != null ? (int) type.getValue() : null;
    }

    public static ByteBuffer getNodes(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(NODES);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static Integer getNum(BencodedDictionary map) {
        BencodedInteger bo = (BencodedInteger) map.get("num");
        return bo != null ? (int) bo.getValue() : null;
    }

    public static Integer getPort(BencodedDictionary map) {
        BencodedInteger bo = (BencodedInteger) map.get(PORT);
        return bo != null ? (int) bo.getValue() : null;
    }

    public static String getQuery(BencodedDictionary map) {
        BencodedByteSequence type = (BencodedByteSequence) map.get(QUERY);
        return type != null ? type.toUTF8String() : null;
    }

    public static BencodedDictionary getResponse(BencodedDictionary map) {
        return (BencodedDictionary) map.get(RESPONSE);
    }

    public static ByteBuffer getSamples(BencodedDictionary map) {
        BencodedByteSequence type = (BencodedByteSequence) map.get("samples");
        return type != null ? ByteBuffer.wrap(type.getByteSequence()) : null;
    }

    public static ByteBuffer getTarget(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(TARGET);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static ByteBuffer getToken(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(TOKEN);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static ByteBuffer getTransaction(BencodedDictionary map) {
        BencodedByteSequence target = (BencodedByteSequence) map.get(TRANSACTIONID);
        return target != null ? ByteBuffer.wrap(target.getByteSequence()) : null;
    }

    public static String getType(BencodedDictionary map) {
        BencodedByteSequence type = (BencodedByteSequence) map.get(TYPE);
        return type != null ? type.toUTF8String() : null;
    }

    public static BencodedList getValues(BencodedDictionary args) {
        return (BencodedList) args.get(VALUES);
    }

    public static class Commands {
        public static final String PING = "ping";
        public static final String FIND_NODE = "find_node";
        public static final String GET_PEERS = "get_peers";
        public static final String ANNOUNCE_PEER = "announce_peer";
        public static final String SAMPLE_INFOHASHES = "sample_infohashes";
    }
}
