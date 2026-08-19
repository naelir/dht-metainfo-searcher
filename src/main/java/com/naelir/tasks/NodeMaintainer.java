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
        if (data.arguments.onlyHashes) {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new UpdateSeenNoPeersTorrentsTask(data));
            tasks.offer(new NextIdTask(data));
        } else if (data.arguments.scrape) {
            tasks.offer(new CreateScrapeHashesTask(data));
            tasks.offer(new ScrapeTask(client, data));
            tasks.offer(new WaitScrapeTask());
            tasks.offer(new UpdateSeenTorrentsTask(data));
//            tasks.offer(new TerminateScrapeTask(data));
        } else {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new UpdateSeenNoPeersTorrentsTask(data));
//            tasks.offer(new SampleRoutingTableTask(client, data));
            GetPeersTask gpt = new GetPeersTask(client, data);
            CreateMetaTask ct = new CreateMetaTask(data);
            UdpTorrentResolverTask trt = new UdpTorrentResolverTask(client, data.udptasks);
//            TcpTorrentResolverTask ttrt = new TcpTorrentResolverTask(tcp, data.tcptasks);
            tasks.offer(new ITask() {
                @Override
                public boolean resolved() {
                    return gpt.resolved() && trt.resolved();// && ttrt.resolved();
                }

                @Override
                public void run() {
                    gpt.run();
                    ct.run();
                    trt.run();
                    //ttrt.run();
                }
            });
            tasks.offer(new NextIdTask(data));
        }
        return new NodeMaintainer(tasks);
    }

    private Queue<ITask> tasks;
    private ITask currentTask;

    public NodeMaintainer(Queue<ITask> tasks) {
        this.tasks = tasks;
        this.currentTask = tasks.poll();
    }

    @Override
    public void run() {
        if (this.currentTask != null) {
            this.currentTask.run();
            if (this.currentTask.resolved()) {
                this.tasks.offer(this.currentTask);
                logger.info("task {} resolved", this.currentTask.getClass().getSimpleName());
                this.currentTask = this.tasks.poll();
            }
        }
    }
}
