package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Torrent;
import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.dht.ITask;
import com.naelir.dht.Node;

public class ReadSampleInfohashesTask implements ITask {
    public static final Logger logger = LogManager.getLogger(ReadSampleInfohashesTask.class);

    private Data data;

    public ReadSampleInfohashesTask(Data data) {
        this.data = data;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public void run() {
        try {
            String hex = Generator.toHex(data.myself.array()).substring(0, 2);
            List<String> searchable = data.fileManager.unresolved()
                    .stream()
                    .filter(e -> e.substring(0, 2).equals(hex))
                    .toList();
            int i = 0;
            for (String key : searchable) {
                String found = data.fileManager.get(key);
                if (found != null) {
                    i++;
                    continue;
                }
                byte[] array = Generator.toArray(key);

                List<Node> closest = data.table.closest(ByteBuffer.wrap(array), 2);
                Sample value = new Sample(new Torrent(key), closest, false);
                if (closest.isEmpty() == false) {
                    data.samples.putIfAbsent(key, value);                    
                }
            }
            logger.info("filter {} hashes, resolved {}, prefix {}", searchable.size(), i, hex);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
