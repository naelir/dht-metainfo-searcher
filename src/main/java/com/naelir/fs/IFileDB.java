package com.naelir.fs;

import java.util.List;

import com.naelir.bt.Entry;

public interface IFileDB extends AutoCloseable {
    void close() throws Exception;

    void insert(Entry fr);

    void fine(Entry fr);

    void insertUnresolved(String hash);

    String get(String hash);

    List<String> scrape();

    List<String> unresolved();
}