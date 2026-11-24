package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;

public class FilterRoutingTableTask implements ITask {
    public static final Logger logger = LogManager.getLogger(FilterRoutingTableTask.class);
    private Data data;

    public FilterRoutingTableTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        var list = data.table.nodes().stream().filter(e -> e.have(Command.PING_R) == false).toList();
        list.forEach(e -> {
            data.table.remove(e.id());
        });
        logger.info("table nodes after {}", data.table.nodes().size());
    }
}
