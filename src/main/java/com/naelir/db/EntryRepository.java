package com.naelir.db;

import java.util.List;

import com.naelir.bt.Entry;

public interface EntryRepository {
    List<Entry> findAll(int page, int pageSize);
    long count();
    Entry findByHash(String hash);
    List<Entry> findByName(String pattern);
    Entry insert(Entry entry);
    boolean update(Entry entry);
    boolean remove(String hash);
    long updateMany(List<String> hashes, int newPeerCount);
    long updateMany(List<String> hashes);
}