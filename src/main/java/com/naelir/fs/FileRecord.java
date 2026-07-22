package com.naelir.fs;

import java.util.Objects;

import com.naelir.bt.TorrentMeta;

/**
 * Entity stored in the CSV file (columns: id#name).
 */
public class FileRecord {
    /** String value matching {@link com.naelir.bt.TorrentMeta#EMPTY}. */
    private static final TorrentMeta DEFAULT_META = new TorrentMeta("0000000000000000000000000000000000000000");
    static final String DEFAULT_NAME = "NAME";
    private final String id;
    private String name;
    private TorrentMeta meta;

    /**
     * Convenience constructor – name defaults to {@code "NAME"}, meta defaults to
     * {@code "EMPTY"}.
     */
    public FileRecord(String id) {
        this(id, DEFAULT_NAME, DEFAULT_META);
    }

    /**
     * Convenience constructor – meta defaults to {@code "EMPTY"} (matches
     * {@link com.naelir.bt.TorrentMeta#EMPTY}).
     */
    public FileRecord(String id, String name) {
        this(id, name, DEFAULT_META);
    }

    /** Full constructor. */
    public FileRecord(String id, String name, TorrentMeta meta) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.meta = Objects.requireNonNull(meta, "meta must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FileRecord r))
            return false;
        return this.id.equals(r.id);
    }

    public String getId() {
        return this.id;
    }

    public TorrentMeta getMeta() {
        return this.meta;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public void setMeta(TorrentMeta meta) {
        this.meta = Objects.requireNonNull(meta);
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        return "FileRecord{id='" + this.id + "', name='" + this.name + "', meta='" + this.meta + "'}";
    }
}