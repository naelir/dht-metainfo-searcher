package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CompactInfo {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] compact(byte[] nodeId, byte[] ip, int port) {
        ByteBuffer cibb = ByteBuffer.allocate(nodeId.length + ip.length + 2);
        cibb.put(nodeId);
        cibb.put(ip);
        cibb.put((byte) ((port >> 8) & 0xff));
        cibb.put((byte) (port & 0xff));
        return cibb.array();
    }

    public static ByteBuffer compactNodes(List<Node> list) {
        Node n1 = list.get(0);
        byte[] compact = compact(n1.id.array(), n1.ip, n1.port);
        int size = list.size();
        ByteBuffer nodes = ByteBuffer.allocate(compact.length * size);
        for (Node node : list) {
            nodes.put(compact(node.id.array(), node.ip, node.port));
        }
        return nodes;
    }

    static ByteBuffer compactPeer(byte[] nodeId, byte[] ip, int port) {
        ByteBuffer cibb = ByteBuffer.allocate(nodeId.length + ip.length + 2);
        cibb.put(nodeId);
        cibb.put(ip);
        cibb.put((byte) ((port >> 8) & 0xff));
        cibb.put((byte) (port & 0xff));
        return cibb;
    }

    public static List<ByteBuffer> compactPeers(List<Node> list) {
        Node n1 = list.get(0);
        byte[] compact = compact(n1.id.array(), n1.ip, n1.port);
        int size = list.size();
        List<ByteBuffer> peers = new ArrayList<>(size);
        for (Node node : list) {
            peers.add(compactPeer(node.id.array(), node.ip, node.port));
        }
        return peers;
    }

    public static List<Node> expand(List<ByteBuffer> info) {
        int count = info.size();
        List<Node> list = new ArrayList<>(count);
        for (ByteBuffer buffer : info) {
            list.add(expandNode(buffer));
        }
        return list;
    }

    public static List<ByteBuffer> expandHashes(ByteBuffer info) {
        int count = info.array().length / 20;
        List<ByteBuffer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] idBytes = new byte[20];
            info.get(idBytes);
            list.add(ByteBuffer.wrap(idBytes));
        }
        return list;
    }

    public static Node expandNode(ByteBuffer info) {
        byte[] idBytes = new byte[20];
        info.get(idBytes);
        byte[] ip = new byte[4];
        info.get(ip);
        byte[] portBytes = new byte[2];
        info.get(portBytes);
        ByteBuffer id = ByteBuffer.wrap(idBytes);
        int port = ((portBytes[0] & 0xFF) << 8) | (portBytes[1] & 0xFF);
        return new Node(ip, port, id);
    }

    public static List<Node> expandNodes(ByteBuffer info) {
        int count = info.array().length / 26;
        List<Node> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] idBytes = new byte[20];
            info.get(idBytes);
            byte[] ip = new byte[4];
            info.get(ip);
            byte[] portBytes = new byte[2];
            info.get(portBytes);
            ByteBuffer id = ByteBuffer.wrap(idBytes);
            int port = ((portBytes[0] & 0xFF) << 8) | (portBytes[1] & 0xFF);
            list.add(new Node(ip, port, id));
        }
        return list;
    }
}
