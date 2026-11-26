package com.naelir.fs;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.bt.Entry;

/**
 * SQLite-backed implementation of {@link IFileDB}.
 * Persists {@link Entry} objects in a single table named "hashes".
 * Only close(), insert() and get() are implemented; the remaining
 * methods throw {@link UnsupportedOperationException}.
 */
public class SqliteFileDB implements IFileDB {
    static final Logger logger = LogManager.getLogger(SqliteFileDB.class);

    private static final String TABLE_DDL = "CREATE TABLE IF NOT EXISTS hashes ("
            + "h TEXT PRIMARY KEY, "
            + "n TEXT, "
            + "fc INTEGER, "
            + "sz LONG, "
            + "g TEXT "
            + ")";

    private static final String INSERT_SQL = "INSERT INTO hashes "
            + "(h, n, fc, sz, g) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT(h) DO UPDATE SET "
            + "n=excluded.n, fc=excluded.fc, "
            + "sz=excluded.sz, g=excluded.g";

    private static final String SELECT_SQL = "SELECT h, n, fc, sz, g "
            + "FROM hashes WHERE h = ?";

    private final Connection connection;

    private final IFileDB fileDb;

    public SqliteFileDB(String dbFilePath) throws ClassNotFoundException, IOException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        try (Statement st = connection.createStatement()) {
            st.execute(TABLE_DDL);
        }
        this.fileDb = FileDB.of();

    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        fileDb.close();
    }

    @Override
    public void insert(Entry fr) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            ps.setString(1, fr.hash);
            ps.setString(2, fr.name);
            ps.setInt(3, fr.fileCount);
            ps.setLong(4, fr.size);
            ps.setString(5, fr.genre);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert entry {}", fr.hash, e);
        }
    }

    @Override
    public Entry get(String hash) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_SQL)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Entry(
                        rs.getString("n"),
                        rs.getString("h"),
                        rs.getInt("fc"),
                        0,
                        rs.getLong("sz"),
                        rs.getString("g"),
                        0);
            }
        } catch (SQLException e) {
           logger.error("Failed to get entry {}", hash, e);
           return null;
        }
    }

    @Override
    public void fine(Entry fr) {
        fileDb.fine(fr);
    }

    @Override
    public void insertUnresolved(String hash) {
        fileDb.insertUnresolved(hash);
    }

    @Override
    public Set<Entry> readScrape(String path) {
        return fileDb.readScrape(path);
    }

    @Override
    public void writeScrape(Set<Entry> set) {
        fileDb.writeScrape(set);
    }

    @Override
    public Set<Entry> readResolved(String substring) {
        return fileDb.readResolved(substring);
    }
}
