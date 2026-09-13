package com.naelir.fs;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.naelir.bt.Entry;

public interface IFileDB extends AutoCloseable {
    void close() throws Exception;

    void insert(Entry fr);

    void fine(Entry fr);

    void insertUnresolved(String hash);

    String get(String hash);

    List<String> readScrape(String path);

    void writeScrape(String path, Set<Pair<String, Integer>> set);

    List<String> unresolved(String path);
}