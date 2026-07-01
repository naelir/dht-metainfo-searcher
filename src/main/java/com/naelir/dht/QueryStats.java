package com.naelir.dht;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class QueryStats {

    private static final int MAX_NOT_RESPONDING_QUERIES = 3;
    
    public Map<Node, Queue<Query>> queryMap;
    
    public QueryStats() {
        this.queryMap = new ConcurrentHashMap<>();
    }

    public void put(Node node, Command command) {
        this.queryMap.computeIfAbsent(node, k -> new LinkedList<>());
        this.queryMap.get(node).add(new Query(command));
    }
    
    public boolean have(Node node, Command command) {
        Queue<Query> queries = this.queryMap.get(node);
        if (queries == null || queries.isEmpty()) {
            return false;
        } else {
            return queries.stream().anyMatch(q -> q.command == command);
        }
        
    }
    

    public Optional<Query> get(Node node, Command command) {
        Queue<Query> queries = this.queryMap.get(node);
        if (queries == null || queries.isEmpty()) {
            return null;
        } else {
            return queries.stream().filter(q -> q.command == command).findFirst();
        }
        
    }
    
    public boolean notResponding(Node node) {
        Queue<Query> queries = this.queryMap.get(node);
        if (queries == null) {
            return false;
        }
        boolean notResponding = true;
        for (Query query : queries) {
            notResponding &= query.notResponding();
        }
        return notResponding;
    }
    

    public boolean haveSlot(Node node) {
        Queue<Query> queries = this.queryMap.get(node);
        if (queries == null) {
            return false;
        } else {
            return queries.size() < MAX_NOT_RESPONDING_QUERIES;
        }

    }
}
