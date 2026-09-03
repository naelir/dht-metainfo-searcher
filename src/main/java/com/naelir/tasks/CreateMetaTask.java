package com.naelir.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Command;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;

public class CreateMetaTask implements ITask {
    public static final Logger logger = LogManager.getLogger(CreateMetaTask.class);
    private Data data;

    public CreateMetaTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("createMetaTasks run on {} samples", this.data.samples.size());
            }
            for (Sample sample : this.data.samples.values()) {
                if (sample.peers().size() < data.arguments.minPeers) {
                    continue;
                }
                for (Node peer : sample.peers()) {
                    if (peer.have(Command.META) == false) {
                        peer.put(Command.META);
                        
                        this.data.udptasks.offer(new MetaTorrentTask(peer, sample.torrent()));
//                            this.data.tcptasks.offer(new MetaTorrentTask(peer, sample.torrent));

                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
