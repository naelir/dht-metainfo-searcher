package com.naelir;

public class Arguments {
    /**
     * Parses command-line arguments.
     * <ul>
     * <li>{@code --max-nodes <int>} – maximum number of closest nodes to collect
     * (default: 100)</li>
     * <li>{@code --post-url <String>} – URL to POST resolved metadata to (default:
     * null)</li>
     * <li>{@code --bitspace-parts <int>} – number of bit-space partitions to
     * explore (default: 100)</li>
     * </ul>
     */
    public static Arguments parse(String[] args) {
        String postUrl = null;
        String from = null;
        int bitspaceParts = 100;
        int maxSamples = 500;
        boolean onlyHashes = false;
        String user = null;
        String pass = null;
        String connectionString = null;
        String db = null;
        String table = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--user":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --user");
                user = args[++i];
                break;
            case "--pass":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --pass");
                pass = args[++i];
                break;
            case "--continue-from":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --max-nodes");
                from = args[++i];
                break;
            case "--post-url":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --post-url");
                postUrl = args[++i];
                break;
            case "--bitspace-parts":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --bitspace-parts");
                bitspaceParts = Integer.parseInt(args[++i]);
                break;
            case "--max-samples":
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("Missing value for --max-samples");
                maxSamples = Integer.parseInt(args[++i]);
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
            default:
                throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return new Builder()
                .bitspaceParts(bitspaceParts)
                .maxSamples(maxSamples)
                .continueFrom(from)
                .onlyHashes(onlyHashes)
                .postUrl(postUrl)
                .user(user)
                .pass(pass)
                .connectionString(connectionString)
                .db(db)
                .table(table)
                .build();
    }

    public final String continueFrom;
    public final int bitspaceParts;
    public final boolean onlyHashes;
    public final String user;
    public final String pass;
    public final String postUrl;
    public final int maxSamples;
    public final String connectionString;
    public final String db;
    public final String table;

    private Arguments(Builder builder) {
        this.bitspaceParts = builder.bitspaceParts;
        this.maxSamples = builder.maxSamples;
        this.continueFrom = builder.continueFrom;
        this.onlyHashes = builder.onlyHashes;
        this.postUrl = builder.postUrl;
        this.user = builder.user;
        this.pass = builder.pass;
        this.connectionString = builder.connectionString;
        this.db = builder.db;
        this.table = builder.table;
    }

    public static class Builder {
        private int bitspaceParts = 100;
        private int maxSamples = 500;
        private String continueFrom;
        private boolean onlyHashes;
        private String postUrl;
        private String user;
        private String pass;
        private String connectionString;
        private String db;
        private String table;

        public Builder bitspaceParts(int bitspaceParts) { this.bitspaceParts = bitspaceParts; return this; }
        public Builder maxSamples(int maxSamples) { this.maxSamples = maxSamples; return this; }
        public Builder continueFrom(String continueFrom) { this.continueFrom = continueFrom; return this; }
        public Builder onlyHashes(boolean onlyHashes) { this.onlyHashes = onlyHashes; return this; }
        public Builder postUrl(String postUrl) { this.postUrl = postUrl; return this; }
        public Builder user(String user) { this.user = user; return this; }
        public Builder pass(String pass) { this.pass = pass; return this; }
        public Builder connectionString(String connectionString) { this.connectionString = connectionString; return this; }
        public Builder db(String db) { this.db = db; return this; }
        public Builder table(String table) { this.table = table; return this; }

        public Arguments build() {
            return new Arguments(this);
        }
    }

    @Override
    public String toString() {
        return "Arguments{bitspaceParts=" + this.bitspaceParts + "}";
    }
}
