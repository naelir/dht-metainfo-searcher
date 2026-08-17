package com.naelir.bt;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naelir.bt.TorrentMeta.Genre;

public class Entry {
    
    public static Entry crap(String hash) {
        return new Entry("chinese_korean_crap", hash, 0, 0, 0, Genre.UNKNOWN.name(), 0);
    }
    
    @JsonProperty("n")
    public String name;

    @JsonProperty("h")
    public String hash;

    @JsonProperty("fc")
    public int fileCount;

    /** Unix epoch milliseconds */
    @JsonProperty("se")
    public long foundTime;
    
    @JsonProperty("sz")
    public long size;
    
    @JsonProperty("g")
    public String genre;
    
    @JsonProperty("p")
    public int peers;
    
    public Entry() {}

    public Entry(String name, String hash, int fileCount, long foundTime, long size, String genre, int peers) {
        this.name = name;
        this.hash = hash;
        this.fileCount = fileCount;
        this.foundTime = foundTime;
        this.size = size;
        this.genre = genre;
        this.peers = peers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Entry other = (Entry) obj;
        return Objects.equals(hash, other.hash);
    }
    
    
}