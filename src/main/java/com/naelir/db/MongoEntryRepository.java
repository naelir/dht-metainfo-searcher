package com.naelir.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.naelir.bt.Entry;

public class MongoEntryRepository implements EntryRepository {

    private final MongoCollection<Document> collection;

    public MongoEntryRepository(String connectionString, String dbName, String collectionName) {
        MongoClient client = MongoClients.create(connectionString);
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
        ensureIndexes();
    }

    private void ensureIndexes() {
        collection.createIndex(Indexes.ascending("h"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("n"));
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
        collection.find(Filters.regex("n", regex))
                .sort(Sorts.descending("_id"))
                .limit(200)
                .forEach(doc -> results.add(fromDocument(doc)));
        return results;
    }

    @Override
    public List<Entry> findAll(int page, int pageSize) {
        int skip = Math.max(0, (page - 1) * pageSize);
        List<Entry> results = new ArrayList<>();
        collection.find()
                .sort(Sorts.descending("_id"))
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
            throw e;
        }
        return entry;
    }

    @Override
    public boolean update(Entry entry) {
        Bson eq = Filters.eq("h", entry.hash);
        Document document = toDocument(entry);
        ReplaceOptions upsert = new ReplaceOptions().upsert(false);
        long modified = collection.replaceOne(eq, document, upsert).getModifiedCount();
        return modified > 0;
    }
    

    @Override
    public long updateMany(List<String> hashes) {
        Bson filter = Filters.in("h", hashes);
        UpdateOptions upsert = new UpdateOptions().upsert(false);
        Bson update = new Document("$set", new Document("se", System.currentTimeMillis()));
        UpdateResult result = collection.updateMany(filter, update, upsert);
        return result.getMatchedCount();
    }

    @Override
    public Entry findByHash(String hash) {
        Document doc = collection.find(Filters.eq("h", hash)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    @Override
    public boolean remove(String hash) {
        long deleted = collection.deleteOne(Filters.eq("h", hash)).getDeletedCount();
        return deleted > 0;
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Document toDocument(Entry e) {
        return new Document("h", e.hash)
                .append("n", e.name)
                .append("g", e.genre)
                .append("fc", e.fileCount)
                .append("se", e.foundTime)
                .append("sz", e.size);
    }

    private static Entry fromDocument(Document doc) {
        Entry e = new Entry();
        e.name      = doc.getString("n");
        e.hash      = doc.getString("h");
        e.genre      = doc.getString("g");
        Integer fc  = doc.getInteger("fc");
        e.fileCount = fc != null ? fc : 0;
        e.foundTime = toLong(doc.get("se"));
        e.size       = toLong(doc.get("sz"));
        return e;
    }

    /** MongoDB can store numbers as Integer or Long depending on the value. */
    private static long toLong(Object val) {
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}