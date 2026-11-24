package com.naelir.tasks;

import java.util.Deque;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Data;
import com.naelir.dht.ITask;
import com.naelir.utp.UtpClient;

public class NodeMaintainer implements Runnable {
    public static final Logger logger = LogManager.getLogger(NodeMaintainer.class);

    private Deque<ITask> tasks;
    private Data data;
    private UtpClient client;

    public NodeMaintainer(UtpClient client, Data data) {
        this.client = client;
        this.tasks = new LinkedList<>();
        this.data = data;
    }

    @Override
    public void run() {
        if (this.data.udptasks.size() > 10)
            return;
        ITask poll = tasks.poll();
        if (poll != null) {
            try {
                poll.run();
                if (poll.resolved()) {
                    logger.info("task {} resolved", poll.getClass().getSimpleName());
                } else {
                    tasks.addFirst(poll);
                }
            } catch (Exception e) {
                logger.error("task {} failed, will retry next tick: {}", poll.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        } else {
            tasks = tasks(data, client);
        }
    }
    


    Deque<ITask> tasks(Data data, UtpClient client) {
        Deque<ITask> tasks = new LinkedList<>();
        if (data.config.mode == 0) {// normal search rotation
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new ScrapeSamplesTask(data));

            tasks.offer(new ScrapeTask(client, data));
            tasks.offer(new WaitScrapeTask(data));
            tasks.offer(new FilterSamplesTask(data));
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

            tasks.offer(new NextIdTask(client, data));
            tasks.offer(new FireNextIdTask(client, data, true));
        } else if (data.config.mode == 1) { // scrape
            tasks.offer(new CreateScrapeHashesTask(data));
            tasks.offer(new ScrapeTask(client, data));
            tasks.offer(new WaitScrapeTask(data));
            tasks.offer(new UpdateSeenTorrentsTask(data));
        } else if (data.config.mode == 2) { // scrape unresolved
            tasks.offer(new CreateScrapeHashesTask(data));
            tasks.offer(new ScrapeTask(client, data));
            tasks.offer(new WaitScrapeTask(data));
            tasks.offer(new SaveScrapedTorrentsTask(data));
        } else if (data.config.mode == 3) { // ban old torrents 
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new FindSampleInfohashesTask(client, data));
            tasks.offer(new ScrapeSamplesTask(data));
            boolean trackerScrape = data.scrapeHashes.size() > data.config.scrapeStep;
            if (trackerScrape) {
                tasks.offer(new ScrapeTask(client, data));
                tasks.offer(new WaitScrapeTask(data));
                tasks.offer(new FilterSamplesTask(data));
            }
            tasks.offer(new NextIdTask(client, data));
            tasks.offer(new FireNextIdTask(client, data, trackerScrape));

        } else if (data.config.mode == 4) {// rotate old torrents
            tasks.offer(new FindNodeTask(client, data));
            tasks.offer(new PingTask(client, data));
            tasks.offer(new FilterRoutingTableTask(data));
            
            tasks.offer(new LoadLowPeersHashesTask(data));
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
            tasks.offer(new NextIdTask(client, data));
            tasks.offer(new FireNextIdTask(client, data, false));
        }
        return tasks;
    }
}
