package com.naelir.tasks;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Converter;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.utp.UtpClient;

public class NextIdTask implements ITask {
    public static final Logger logger = LogManager.getLogger(NextIdTask.class);
    private Data data;
    private ByteBuffer nextId;
    private UtpClient client;

    public NextIdTask(UtpClient client, Data data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        this.nextId = this.data.nextId();
        String myself = Converter.toHex(this.nextId.array());
        int j = 0;
        int k = 0;
        int r = 0;
        for (Sample sample : this.data.samples.values()) {
            if (sample.torrent.meta() != null) {
                r++;
            } else if (sample.skip) {
                j++;
            } else {
                k++;
            }
            logger.debug("{}", sample);
        }
        logger.info("next id is {}, crap {}, resolved {}, not {}", myself, j, r, k);

    }
}
