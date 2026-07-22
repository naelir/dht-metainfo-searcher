package com.naelir.db;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.naelir.bt.Entry;

public class MongoEntryRepository implements EntryRepository {
    public static final Logger logger = LogManager.getLogger(MongoEntryRepository.class);

    private final MongoCollection<Document> collection;

    public MongoEntryRepository(String connectionString, String dbName, String collectionName) {
        MongoClient client = MongoClients.create(connectionString);
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
        ensureIndexes();
    }

    /**
     * Creates the required indexes if they do not already exist.
     *
     * <ul>
     *   <li>Unique index on {@code hash}     – enforces deduplication and drives findByHash.</li>
     *   <li>Descending index on {@code foundTime} – speeds up the sort in {@link #findAll}.</li>
     * </ul>
     */
    private void ensureIndexes() {
        collection.createIndex(Indexes.ascending("hash"), new IndexOptions().unique(true).sparse(true));
        collection.createIndex(Indexes.descending("foundTime"));
        // speeds up prefix queries; arbitrary contains-searches are a full-scan in any DB
        collection.createIndex(Indexes.ascending("name"));
    }

    // ...existing code...

    @Override
    public List<Entry> findByName(String pattern) {
        if (pattern == null || pattern.trim().length() < 3)
            return List.of();
        // Split on spaces so that each space acts as a wildcard matching any sequence of characters.
        String[] parts = pattern.split(" ", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".*");
            sb.append(Pattern.quote(parts[i]));
        }
        Pattern regex = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        List<Entry> results = new ArrayList<>();
        collection.find(Filters.regex("name", regex))
                .sort(new Document("foundTime", -1))
                .limit(200)
                .forEach(doc -> results.add(fromDocument(doc)));
        return results;
    }

    @Override
    public List<Entry> findAll(int page, int pageSize) {
        int skip = Math.max(0, (page - 1) * pageSize);
        List<Entry> results = new ArrayList<>();
        collection.find()
                .sort(new Document("foundTime", -1))
                .skip(skip)
                .limit(pageSize)
                .forEach(doc -> results.add(fromDocument(doc)));
        return results;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public Entry insert(Entry entry) {
        try {
            collection.insertOne(toDocument(entry));
        } catch (MongoWriteException e) {
            if (e.getCode() == 11000) {
                return entry;   // duplicate hash – skip silently
            }
            logger.error("Failed to insert entry with hash {}: ", entry.hash, e);

        }
        return entry;
    }

    @Override
    public boolean update(Entry entry) {
        Bson eq = Filters.eq("hash", entry.hash);
        Document document = toDocument(entry);
        ReplaceOptions upsert = new ReplaceOptions().upsert(false);
        long modified = collection.replaceOne(eq, document, upsert).getModifiedCount();
        return modified > 0;
    }

    @Override
    public Entry findByHash(String hash) {
        Document doc = collection.find(Filters.eq("hash", hash)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    @Override
    public boolean remove(String hash) {
        long deleted = collection.deleteOne(Filters.eq("hash", hash)).getDeletedCount();
        return deleted > 0;
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Document toDocument(Entry e) {
        return new Document("hash", e.hash)
                .append("name", e.name)
                .append("genre", e.genre)
                .append("fileCount", e.fileCount)
                .append("foundTime", e.foundTime)
                .append("size", e.size);
    }

    private static Entry fromDocument(Document doc) {
        Entry e = new Entry();
        e.name      = doc.getString("name");
        e.hash      = doc.getString("hash");
        e.genre      = doc.getString("genre");
        Integer fc  = doc.getInteger("fileCount");
        e.fileCount = fc != null ? fc : 0;
        e.foundTime = toLong(doc.get("foundTime"));
        e.size       = toLong(doc.get("size"));
        return e;
    }

    /** MongoDB can store numbers as Integer or Long depending on the value. */
    private static long toLong(Object val) {
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}