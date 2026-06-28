package com.naelir;

public class Arguments {
    public final int maxNodes;
    public final String postUrl;
    public final String continueFrom;
    public final int bitspaceParts;

    Arguments(int maxNodes, String postUrl, int bitspaceParts, String continueFrom) {
        this.maxNodes = maxNodes;
        this.postUrl = postUrl;
        this.bitspaceParts = bitspaceParts;
        this.continueFrom = continueFrom;
    }

    /**
     * Parses command-line arguments.
     * <ul>
     *   <li>{@code --max-nodes <int>}       – maximum number of closest nodes to collect (default: 100)</li>
     *   <li>{@code --post-url <String>}     – URL to POST resolved metadata to (default: null)</li>
     *   <li>{@code --bitspace-parts <int>}  – number of bit-space partitions to explore (default: 100)</li>
     * </ul>
     */
    public static Arguments parse(String[] args) {
        int maxNodes = 100;
        String postUrl = null;
        String from = null;
        int bitspaceParts = 100;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--max-nodes":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --max-nodes");
                    maxNodes = Integer.parseInt(args[++i]);
                    break;
                case "--continue-from":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --max-nodes");
                    from = args[++i];
                    break;
                case "--post-url":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --post-url");
                    postUrl = args[++i];
                    break;
                case "--bitspace-parts":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --bitspace-parts");
                    bitspaceParts = Integer.parseInt(args[++i]);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }

        return new Arguments(maxNodes, postUrl, bitspaceParts, from);
    }

    @Override
    public String toString() {
        return "Arguments{maxNodes=" + maxNodes + ", postUrl=" + postUrl + ", bitspaceParts=" + bitspaceParts + "}";
    }
}
