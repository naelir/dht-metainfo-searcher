package com.naelir.dht;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.BtTcpClient;
import com.naelir.utp.NettyUtpClient;

import io.netty.util.concurrent.DefaultThreadFactory;

public class NodeMaintainer implements Runnable, AutoCloseable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);

    public static NodeMaintainer of(Data data, NettyUtpClient client, BtTcpClient tcp, Semaphore semaphore)
            throws Exception {
        Queue<ITask> tasks = new LinkedList<>();
        if (data.arguments.onlyHashes) {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new NextIdTask(data));
        } else {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
//            tasks.offer(new RemoveNoPeersTask(data));
//            tasks.offer(new PingPeersTask(client, data));
            GetPeersTask gpt = new GetPeersTask(client, data);
            CreateMetaTask ct = new CreateMetaTask(data);
            UdpTorrentResolverTask trt = new UdpTorrentResolverTask(client, data.tasks);
            TcpTorrentResolverTask ttrt = new TcpTorrentResolverTask(tcp, data.tcptasks);
            tasks.offer(new ITask() {
                @Override
                public boolean resolved() {
                    return gpt.resolved() && trt.resolved() && ttrt.resolved();
                }

                @Override
                public void run() {
                    gpt.run();
                    ct.run();
                    trt.run();
                    ttrt.run();
                }
            });
            tasks.offer(new NextIdTask(data));
        }
        return new NodeMaintainer(tasks, data, semaphore);
    }

    private Queue<ITask> tasks;
    private ITask currentTask;
    private ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(new DefaultThreadFactory("scheduler"));
    private Data data;
    private Semaphore semaphore;

    public NodeMaintainer(Queue<ITask> tasks, Data data, Semaphore semaphore) {
        this.tasks = tasks;
        this.data = data;
        this.semaphore = semaphore;
        this.currentTask = tasks.poll();
    }

    @Override
    public void close() throws Exception {
        this.executor.shutdown();
    }

    @Override
    public void run() {
        if (this.currentTask != null && this.data.myself != null) {
            this.currentTask.run();
            if (this.currentTask.resolved()) {
                this.tasks.offer(this.currentTask);
                logger.info("task {} resolved", this.currentTask.getClass().getSimpleName());
                this.currentTask = this.tasks.poll();
            }
        } else {
            this.semaphore.release();
        }
    }

    public void start() {
        this.executor.scheduleAtFixedRate(this, 0, 5, TimeUnit.SECONDS);
    }
}
