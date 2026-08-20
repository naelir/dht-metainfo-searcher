package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.dht.RoutingTable;

public class NextIdTask implements ITask {
    public static final Logger logger = LogManager.getLogger(NextIdTask.class);

    private Data data;

    private ByteBuffer nextId;

    public NextIdTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        nextId = data.nextId();
        if (nextId != null) {
            String myself = Generator.toHex(nextId.array());
            logger.warn("next id is {}", myself);
            List<Node> nodes = this.data.table.closest(nextId, 20);
            this.data.table = new RoutingTable();
            nodes.forEach(e -> {
                e.queries.clear();
                this.data.table.insert(e);
            });
            this.data.samples.clear();
            this.data.torrents.clear();
        }
    }
    
    @Override
    public boolean stop() {
        return nextId == null;
    }
}
