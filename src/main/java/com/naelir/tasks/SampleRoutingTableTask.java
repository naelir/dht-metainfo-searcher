package com.naelir.tasks;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;
import com.naelir.utp.UtpClient;

public class SampleRoutingTableTask implements ITask {
    public static final Logger logger = LogManager.getLogger(SampleRoutingTableTask.class);
    private Data data;
    private UtpClient client;

    boolean resolved;
    
    public SampleRoutingTableTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return this.data.samples.values()
                .stream()
                .allMatch(e -> e.from.have(Command.SAMPLE_FN));
    }

    @Override
    public void run() {
        try {
            Collection<Sample> samples = this.data.samples.values();
            int step = 20;
            int i = 0;
            for (Sample sample : samples) {
                if (step < 0) {
                    break;
                }
                if (sample.from.have(Command.SAMPLE_FN) == false) {
                    i++;
                    step--;
                    sample.from.put(Command.SAMPLE_FN);
                    this.client.sendFindNode(this.data.myself, sample.from.id(), sample.from);
                }
            }
            logger.info("sample findNodes: to {} nodes", i);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
