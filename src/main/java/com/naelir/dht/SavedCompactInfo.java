package com.naelir.dht;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public class SavedCompactInfo implements java.io.Serializable {
    
    public static SavedCompactInfo empty() {
        return new SavedCompactInfo(new byte[0], List.of());
    }
    
    public static SavedCompactInfo of(ByteBuffer myself, List<Node> nodes) {
        byte[] myselfBytes = new byte[myself.remaining()];
        myself.get(myselfBytes);
        List<SavedNode> savedNodes = nodes.stream()
                .map(node -> new SavedNode(node.id.array(), node.ip, node.port))
                .toList();
        return new SavedCompactInfo(myselfBytes, savedNodes);
    }
    
    public static List<Node> nodes(SavedCompactInfo info) {
        return  info.nodes.stream()
                .map(savedNode -> new Node(savedNode.ip, savedNode.port))
                .toList();
    }
    
    private static final long serialVersionUID = -4652879924708586490L;
    public byte[] myself;
    public List<SavedNode> nodes;
    
    public SavedCompactInfo(byte[] myself, List<SavedNode> nodes) {
        this.myself = myself;
        this.nodes = nodes;
    }
    
    static class SavedNode implements Serializable {
        private static final long serialVersionUID = 965626653414133722L;
        byte[] id;
        byte[] ip;
        int port;

        public SavedNode(byte[] id, byte[] ip, int port) {
            this.id = id;
            this.ip = ip;
            this.port = port;
        }
    }

}
