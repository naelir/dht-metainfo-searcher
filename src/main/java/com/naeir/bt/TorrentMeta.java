package com.naeir.bt;

import java.nio.ByteBuffer;
import java.util.List;

import com.naelir.dht.CompactInfo;
import com.naelir.dht.From;

public class TorrentMeta {
    private String name;
    private List<MetaFile> list;
    private From from;
    private ByteBuffer torrent;
    long found;

    public TorrentMeta(ByteBuffer torrent, String name, List<MetaFile> list, From from) {
        this.torrent = torrent;
        this.name = name;
        this.list = list;
        this.from = from;
        this.found = System.currentTimeMillis();
    }

    public long getFound() {
        return this.found;
    }

    public List<MetaFile> getList() {
        return this.list;
    }

    public String getName() {
        return this.name;
    }

    public String getTorrent() {
        return CompactInfo.bytesToHex(this.torrent.array());
    }

    @Override
    public String toString() {
        return "TorrentMeta [torrent=" + this.torrent + ", name=" + this.name + ", list=" + this.list + ", from="
                + this.from + "]";
    }

    public static final class MetaFile {
        String path;
        long bytes;

        public MetaFile(String path, long bytes) {
            this.path = path;
            this.bytes = bytes;
        }

        public long getBytes() {
            return this.bytes;
        }

        public String getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            return "MetaFile [path=" + this.path + ", bytes=" + this.bytes + "]";
        }
    }
}
