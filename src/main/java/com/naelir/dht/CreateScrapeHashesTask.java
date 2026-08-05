package com.naelir.dht;

public class CreateScrapeHashesTask implements ITask {

    private Data data;

    public CreateScrapeHashesTask(Data data) {
        this.data = data;
    }

    @Override
    public void run() {
        int i = 0;
        for (String hash : data.torrents.keySet()) {
            data.scrapeHashes.add(hash);
            if (i >= 1000) {
                break;
            }
        }

    }

    @Override
    public boolean resolved() {
        return true;
    }

}
