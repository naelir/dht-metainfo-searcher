package com.naelir.db;

import java.util.List;

import com.naelir.bt.Entry;

public interface EntryRepository {

    /** Returns a page of entries sorted by foundTime descending. */
    List<Entry> findAll(int page, int pageSize);

    /** Total number of entries. */
    long count();

    /** Find single entry by hash, or {@code null} if not found. */
    Entry findByHash(String hash);

    /**
     * Returns entries whose {@code name} contains {@code pattern} (case-insensitive),
     * equivalent to SQL {@code LIKE '%pattern%'}.
     */
    List<Entry> findByName(String pattern);

    /** Inserts a new entry and returns the stored instance. */
    Entry insert(Entry entry);

    /**
     * Replaces an existing entry identified by its hash.
     *
     * @return {@code true} if an entry was actually replaced.
     */
    boolean update(Entry entry);

    /**
     * Removes an existing entry identified by its hash.
     *
     * @return {@code true} if an entry was actually removed.
     */
    boolean remove(String hash);
}