package com.naelir.dht;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.utp.NettyUtpClient;

import io.netty.util.concurrent.DefaultThreadFactory;

public class NodeMaintainer implements Runnable, AutoCloseable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);

    public static NodeMaintainer of(Data data, NettyUtpClient client, Semaphore semaphore) throws Exception {
        Queue<ITask> tasks = new LinkedList<>();
        if (data.arguments.onlyHashes) {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new NextIdTask(data));
        } else {
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new GetPeersTask(client, data));
            tasks.offer(new RemoveNoPeersTask(data));
//            tasks.offer(new PingPeersTask(client, data));
            tasks.offer(new CreateMetaTask(data));
            tasks.offer(new TorrentResolverTask(client, data));
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
                logger.info("task {} resolved", this.currentTask.getClass().getSimpleName());
                this.currentTask = this.tasks.poll();
                this.tasks.offer(this.currentTask);
            }
        } else {
            this.semaphore.release();
        }
    }

    public void start() {
        this.executor.scheduleAtFixedRate(this, 0, 5, TimeUnit.SECONDS);
    }
}
