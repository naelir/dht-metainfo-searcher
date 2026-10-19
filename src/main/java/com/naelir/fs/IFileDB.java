package com.naelir.fs;

import java.util.Set;

import com.naelir.bt.Entry;

public interface IFileDB extends AutoCloseable {
    void close() throws Exception;

    void insert(Entry fr);

    void fine(Entry fr);

    void insertUnresolved(String hash);

    String get(String hash);

    Set<Entry> readScrape(String path);

    void writeScrape(String path, Set<Entry> set);
}