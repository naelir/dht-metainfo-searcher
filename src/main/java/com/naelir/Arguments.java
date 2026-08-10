package com.naelir;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Arguments {
    /**
     * Parses command-line arguments.
     * <ul>
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
     * <li>{@code --query-count <int>} – number of queries to perform per hash
     * (default: 1)</li>
     * <li>{@code --min-peers <int>} – minimum number of peers required before
     * resolving metadata (default: 1)</li>
     * <li>{@code --scrape} – enable scraping of peer counts from trackers
     * (default: false)</li>
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
        int queryCount = 1;
        int minPeers = 1;
        int maxNodes = 600;
        int scrapeStep = 5000;
        boolean scrape = false;
        InetAddress trackerUrl = null;
        int trackerPort = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
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
            case "--query-count":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --query-count");
                queryCount = Integer.parseInt(args[++i]);
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
                .queryCount(queryCount)
                .minPeers(minPeers)
                .scrape(scrape)
                .trackerUrl(trackerUrl)
                .trackerPort(trackerPort)
                .maxNodes(maxNodes)
                .scrapeStep(scrapeStep)
                .build();
    }

    public final String continueFrom;
    public final int bitspaceParts;
    public final boolean onlyHashes;
    public final String connectionString;
    public final String db;
    public final String table;
    public final int queryCount;
    public final int minPeers;
    public final boolean scrape;
    public final InetAddress trackerUrl;
    public final int trackerPort;
    public final int scrapeStep;
    public final int maxNodes;

    private Arguments(Builder builder) {
        this.bitspaceParts = builder.bitspaceParts;
        this.continueFrom = builder.continueFrom;
        this.onlyHashes = builder.onlyHashes;
        this.connectionString = builder.connectionString;
        this.db = builder.db;
        this.table = builder.table;
        this.queryCount = builder.queryCount;
        this.minPeers = builder.minPeers;
        this.scrape = builder.scrape;
        this.trackerUrl = builder.trackerUrl;
        this.trackerPort = builder.trackerPort;
        this.maxNodes = builder.maxNodes;
        this.scrapeStep = builder.scrapeStep;
    }

    @Override
    public String toString() {
        return "Arguments{bitspaceParts=" + this.bitspaceParts + "}";
    }

    public static class Builder {
        private int bitspaceParts = 100;
        private String continueFrom;
        private boolean onlyHashes;
        private String connectionString;
        private String db;
        private String table;
        private int queryCount = 1;
        private int minPeers = 1;
        private boolean scrape;
        private InetAddress trackerUrl;
        private int trackerPort;
        private int maxNodes = 300;
        private int scrapeStep = 5000;
        
        public Builder scrapeStep(int scrapeStep) {
            this.scrapeStep = scrapeStep;
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

        public Builder queryCount(int queryCount) {
            this.queryCount = queryCount;
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
    }
}
