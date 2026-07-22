package com.naelir.dht;

import java.util.List;

public class NextIdTask implements ITask {
    private Data data;

    public NextIdTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        this.data.nextId();
        List<Node> nodes = this.data.table.closest(this.data.myself, 20);
        this.data.table = new RoutingTable();
        nodes.forEach(e -> this.data.table.insert(e));
    }
}
