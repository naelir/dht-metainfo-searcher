package com.naelir;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Arguments {
    /**
     * Parses command-line arguments.
     * <ul>
     * <li>{@code --help} – print available options and exit</li>
     * <li>{@code --bitspace-parts <int>} – number of bit-space partitions to
     * explore (default: 100)</li>
     * <li>{@code --continue-from <String>} – hash to continue from (default:
     * null)</li>
     * <li>{@code --only-hashes} – only collect hashes, skip metadata resolution
     * (default: false)</li>
     * <li>{@code --connection-string <String>} – database connection string
     * (default: null)</li>
     * <li>{@code --db <String>} – database name (default: null)</li>
     * <li>{@code --table <String>} – table name (default: null)</li>
     * <li>{@code --get-peers-depth <int>} – number of get-peers queries per hash
     * (default: 1)</li>
     * <li>{@code --min-peers <int>} – minimum number of peers required before
     * resolving metadata (default: 1)</li>
     * <li>{@code --max-nodes <int>} – maximum number of DHT nodes (default: 600)</li>
     * <li>{@code --schedule-interval <int>} – crawl schedule interval in seconds
     * (default: 5)</li>
     * <li>{@code --scrape} – enable scraping of peer counts from trackers
     * (default: false)</li>
     * <li>{@code --scrape-step <int>} – number of hashes between scrape calls
     * (default: 5000)</li>
     * <li>{@code --tracker-url <String>} – tracker host address (default:
     * null)</li>
     * <li>{@code --tracker-port <int>} – tracker port (default: 0)</li>
     * </ul>
     */
    public static Arguments parse(String[] args) {
        String from = null;
        int bitspaceParts = 100;
        boolean onlyHashes = false;
        String connectionString = null;
        String db = null;
        String table = null;
        int getPeerDepth = 3;
        int minPeers = 1;
        int maxNodes = 400;
        int scrapeStep = 2000;
        int hashesStep = 10;
        boolean scrape = false;
        InetAddress trackerUrl = null;
        int trackerPort = 0;
        int scheduleInterval = 5;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--help":
                System.out.println(
                    "Usage: dht-metainfo-searcher [OPTIONS]\n" +
                    "\n" +
                    "Options:\n" +
                    "  --bitspace-parts <int>        Number of bit-space partitions to explore (default: 100)\n" +
                    "  --continue-from <hash>        Hash to continue crawling from (default: none)\n" +
                    "  --only-hashes                 Only collect hashes, skip metadata resolution (default: false)\n" +
                    "  --connection-string <string>  Database connection string\n" +
                    "  --db <string>                 Database name\n" +
                    "  --table <string>              Table name\n" +
                    "  --get-peers-depth <int>       Number of get-peers queries per hash (default: 1)\n" +
                    "  --min-peers <int>             Minimum peers required before resolving metadata (default: 1)\n" +
                    "  --max-nodes <int>             Maximum number of DHT nodes (default: 600)\n" +
                    "  --schedule-interval <int>     Schedule interval in seconds (default: 5)\n" +
                    "  --scrape                      Enable scraping of peer counts from trackers (default: false)\n" +
                    "  --scrape-step <int>           Number of hashes between scrape calls (default: 2000)\n" +
                    "  --tracker-url <host>          Tracker host address\n" +
                    "  --tracker-port <int>          Tracker port (default: 0)\n" +
                    "  --help                        Show this help message and exit\n"
                );
                System.exit(0);
                break;
            case "--continue-from":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --continue-from");
                from = args[++i];
                break;
            case "--bitspace-parts":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --bitspace-parts");
                bitspaceParts = Integer.parseInt(args[++i]);
                break;
            case "--schedule-interval":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --schedule-interval");
                scheduleInterval = Integer.parseInt(args[++i]);
                break;
            case "--only-hashes":
                onlyHashes = true;
                break;
            case "--connection-string":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --connection-string");
                connectionString = args[++i];
                break;
            case "--db":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --db");
                db = args[++i];
                break;
            case "--table":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --table");
                table = args[++i];
                break;
            case "--get-peers-depth":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --get-peers-depth");
                getPeerDepth = Integer.parseInt(args[++i]);
                break;
            case "--min-peers":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --min-peers");
                minPeers = Integer.parseInt(args[++i]);
                break;
            case "--max-nodes":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --max-nodes");
                maxNodes = Integer.parseInt(args[++i]);
                break;
            case "--hashes-step":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --hashes-step");
                hashesStep = Integer.parseInt(args[++i]);
                break;
            case "--scrape":
                scrape = true;
                break;
            case "--scrape-step":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --scrape-step");
                scrapeStep = Integer.parseInt(args[++i]);
                break;
            case "--tracker-url":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --tracker-url");
                try {
                    trackerUrl = InetAddress.getByName(args[++i]);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("Invalid value for --tracker-url: " + args[i], e);
                }
                break;
            case "--tracker-port":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --tracker-port");
                trackerPort = Integer.parseInt(args[++i]);
                break;
            default:
                throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return new Builder().bitspaceParts(bitspaceParts)
                .continueFrom(from)
                .onlyHashes(onlyHashes)
                .connectionString(connectionString)
                .db(db)
                .table(table)
                .getPeersDepth(getPeerDepth)
                .minPeers(minPeers)
                .scrape(scrape)
                .trackerUrl(trackerUrl)
                .trackerPort(trackerPort)
                .maxNodes(maxNodes)
                .scrapeStep(scrapeStep)
                .scheduleInterval(scheduleInterval)
                .hashesStep(hashesStep)
                .build();
    }

    public final String continueFrom;
    public final int bitspaceParts;
    public final boolean onlyHashes;
    public final String connectionString;
    public final String db;
    public final String table;
    public final int getPeersDepth;
    public final int minPeers;
    public final boolean scrape;
    public final InetAddress trackerUrl;
    public final int trackerPort;
    public final int scrapeStep;
    public final int maxNodes;
    public final int scheduleInterval;
    public final String scrapeFile;
    public final int hashesStep;

    private Arguments(Builder builder) {
        this.bitspaceParts = builder.bitspaceParts;
        this.continueFrom = builder.continueFrom;
        this.onlyHashes = builder.onlyHashes;
        this.connectionString = builder.connectionString;
        this.db = builder.db;
        this.table = builder.table;
        this.getPeersDepth = builder.getPeersDepth;
        this.minPeers = builder.minPeers;
        this.scrape = builder.scrape;
        this.trackerUrl = builder.trackerUrl;
        this.trackerPort = builder.trackerPort;
        this.maxNodes = builder.maxNodes;
        this.scrapeStep = builder.scrapeStep;
        this.scheduleInterval = builder.scheduleInterval;
        this.scrapeFile = builder.scrapeFile;
        this.hashesStep = builder.hashesStep;
    }
    

    @Override
    public String toString() {
        return "Arguments [continueFrom=" + continueFrom + ", bitspaceParts=" + bitspaceParts + ", onlyHashes="
                + onlyHashes + ", connectionString=" + connectionString + ", db=" + db + ", table=" + table
                + ", queryCount=" + getPeersDepth + ", minPeers=" + minPeers + ", scrape=" + scrape + ", trackerUrl="
                + trackerUrl + ", trackerPort=" + trackerPort + ", scrapeStep=" + scrapeStep + ", maxNodes=" + maxNodes
                + "]";
    }


    public static class Builder {
        private int bitspaceParts = 100;
        private String continueFrom;
        private boolean onlyHashes;
        private String connectionString;
        private String db;
        private String table;
        private int getPeersDepth = 1;
        private int minPeers = 1;
        private boolean scrape;
        private InetAddress trackerUrl;
        private int trackerPort;
        private int maxNodes = 400;
        private int scrapeStep = 2000;
        private int scheduleInterval = 2;
        private String scrapeFile;
        private int hashesStep = 3;
        
        public Builder scrapeStep(int scrapeStep) {
            this.scrapeStep = scrapeStep;
            return this;
        }
        
        public Builder hashesStep(int hashesStep) {
            this.hashesStep   = hashesStep;
            return this;
        }

        public Builder scheduleInterval(int scheduleInterval) {
            this.scheduleInterval  = scheduleInterval;
            return this;
        }

        public Builder bitspaceParts(int bitspaceParts) {
            this.bitspaceParts = bitspaceParts;
            return this;
        }

        public Builder maxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
            return this;
        }

        public Arguments build() {
            return new Arguments(this);
        }

        public Builder connectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        public Builder continueFrom(String continueFrom) {
            this.continueFrom = continueFrom;
            return this;
        }

        public Builder db(String db) {
            this.db = db;
            return this;
        }

        public Builder onlyHashes(boolean onlyHashes) {
            this.onlyHashes = onlyHashes;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder getPeersDepth(int queryCount) {
            this.getPeersDepth = queryCount;
            return this;
        }

        public Builder minPeers(int minPeers) {
            this.minPeers = minPeers;
            return this;
        }

        public Builder scrape(boolean scrape) {
            this.scrape = scrape;
            return this;
        }

        public Builder trackerUrl(InetAddress trackerUrl) {
            this.trackerUrl = trackerUrl;
            return this;
        }

        public Builder trackerPort(int trackerPort) {
            this.trackerPort = trackerPort;
            return this;
        }

        public Builder scrapeFile(String string) {
            this.scrapeFile = string;
            return this;
        }
    }
}
