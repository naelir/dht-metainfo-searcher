package com.naelir.db;

import java.util.List;

import com.naelir.bt.Entry;

public class NullEntryRepository implements EntryRepository {

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public List<Entry> findAll(int page, int pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Entry findByHash(String hash) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Entry> findByName(String pattern) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Entry insert(Entry entry) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean update(Entry entry) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean remove(String hash) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long updateMany(List<String> hashes, int newPeerCount) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long updateMany(List<String> hashes) {
        // TODO Auto-generated method stub
        return 0;
    }

}
