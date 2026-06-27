package com.naelir.bt;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry {
    
    @JsonProperty("name")
    public String name;

    @JsonProperty("hash")
    public String hash;

    @JsonProperty("fileCount")
    public int fileCount;

    /** Unix epoch milliseconds */
    @JsonProperty("foundTime")
    public long foundTime;
    
    @JsonProperty("nfo")
    public boolean nfo;
    
    @JsonProperty("genre")
    public String genre;

    public Entry() {}

    public Entry(String name, String hash, int fileCount, long foundTime, boolean nfo, String genre) {
        super();
        this.name = name;
        this.hash = hash;
        this.fileCount = fileCount;
        this.foundTime = foundTime;
        this.nfo = nfo;
        this.genre = genre;
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