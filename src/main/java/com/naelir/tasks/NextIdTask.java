package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Entry;
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
            for (Node e : nodes) {
                e.queries.clear();
                this.data.table.insert(e);
            }
            if (data.arguments.getPeersDepth > 1) {
                int i = 0;
                int j = 0;
                int k = 0;
                int r = 0;
                for (Sample sample : data.samples.values()) {
                    if (sample.torrent.meta() != null) {
                        r++;
                    } if (sample.peers().size() <= 3 && sample.skip == false) {
                        i++;
                        data.fileManager.insert(Entry.lowPeers(sample.torrent.infoHash()));
                    } else if (sample.skip) {
                        j++;
                    }  else {
                        k++;  
                        data.fileManager.insertUnresolved(sample.torrent.infoHash());
                    }
                }
                logger.warn("samples; low peers {}, crap {}, resolved {}, not {}", i, j, r, k);
            }
            this.data.samples.clear();
            this.data.torrents.clear();
            this.data.requestsSent.invalidateAll();
            this.data.tokensSent.invalidateAll();
            this.data.tokensReceived.invalidateAll();
        }
    }
    
    @Override
    public boolean stop() {
        return nextId == null;
    }
}
