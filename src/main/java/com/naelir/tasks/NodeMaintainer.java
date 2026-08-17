package com.naelir.tasks;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;
import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.utp.UtpClient;

public class NodeMaintainer implements Runnable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);

    public static NodeMaintainer of(Data data, UtpClient client, BtTcpClient tcp)
            throws Exception {
        Queue<ITask> tasks = new LinkedList<>();
        boolean rotate = true;
        if (data.arguments.mode == 0) {//normal search rotation
                tasks.offer(new FindNodeTask(client, data));
                tasks.offer(new FindSampleInfohashesTask(client, data));
                GetPeersTask gpt = new GetPeersTask(client, data);
                CreateMetaTask ct = new CreateMetaTask(data);
                tasks.offer(new ITask() {
                    @Override
                    public boolean resolved() {
                        return gpt.resolved();
                    }

                    @Override
                    public void run() {
                        gpt.run();
                        ct.run();
                    }
                });
                tasks.offer(new NextIdTask(data));
            
        } else if (data.arguments.mode == 1) { //scrape
            tasks.offer(new CreateScrapeHashesTask(data));
            tasks.offer(new ScrapeTask(client, data));
            tasks.offer(new WaitScrapeTask());
            tasks.offer(new UpdateSeenTorrentsTask(data));
        } else if (data.arguments.mode == 2) {// resolve predefined list of hashes
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new ReadSampleInfohashesTask(data));
            GetPeersTask gpt = new GetPeersTask(client, data);
            CreateMetaTask ct = new CreateMetaTask(data);
            tasks.offer(new ITask() {
                @Override
                public boolean resolved() {
                    return gpt.resolved();
                }

                @Override
                public void run() {
                    gpt.run();
                    ct.run();
                }
            });
            tasks.offer(new NextIdTask(data));
        } else if (data.arguments.mode == 3) {// resolve peers via tracker
            tasks.offer(new TrackerReadSampleInfohashesTask(data));
            tasks.offer(new TrackerFindPeersTask(client, data));
            tasks.offer(new WaitAnnounceTask(data));
            tasks.offer(new CleanAnnounceTask(data));
        } 
        return new NodeMaintainer(tasks, data, rotate);
    }

    private Queue<ITask> tasks;
    private ITask currentTask;
    private boolean rotate;
    private Data data;

    public NodeMaintainer(Queue<ITask> tasks, Data data, boolean rotate) {
        this.tasks = tasks;
        this.data = data;
        this.rotate = rotate;
        this.currentTask = tasks.poll();
    }

    @Override
    public void run() {
        if (data.udptasks.size() > 10) {
            return;
        }
        if (this.currentTask != null) {
            this.currentTask.run();
            if (this.currentTask.resolved()) {
                if (rotate) {
                    this.tasks.offer(this.currentTask);
                }
                logger.info("task {} resolved", this.currentTask.getClass().getSimpleName());
                this.currentTask = this.tasks.poll();
            }
        }
    }
}
