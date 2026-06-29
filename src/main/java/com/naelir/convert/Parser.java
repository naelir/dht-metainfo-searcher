package com.naelir.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
import com.naelir.bt.TorrentMeta;
import com.naelir.bt.TorrentMeta.MetaFile;
import com.naelir.dht.FileManager;

public class Parser {

    private static final Pattern PR_FILE_COUNT = Pattern.compile("<span class=torrent_files style=color:#666;padding-left:10px>(\\d+)</span>");
    private static final Pattern FILE_COUNT = Pattern.compile(".+?&nbsp;(\\d+) hidden");
    private static final Pattern AGO = Pattern.compile("found (.+?)<.+");
    private static final Pattern SIZE = Pattern.compile("([\\d\\.]+)&nbsp;([MBGK]+)");
    private static final Pattern HASH_NAME = Pattern.compile("urn:btih:(.{40}).+?dn=(.+?)&");
    //
    public static Meta parse(String line) {
        Matcher matcher00 = PR_FILE_COUNT.matcher(line);
        Matcher matcher01 = FILE_COUNT.matcher(line);
        Matcher matcher02 = AGO.matcher(line);
        Matcher matcher03 = SIZE.matcher(line);
        Matcher matcher04 = HASH_NAME.matcher(line);
        
        String count0 = matcher00.find() ? matcher00.group(1) : "0";
        String count = matcher01.find() ? matcher01.group(1) : "0";
        String ago = matcher02.find() ? matcher02.group(1) : "";
        String size = matcher03.find() ? matcher03.group(1) : "0";
        String suf = matcher03.find() ? matcher03.group(2) : "";
        boolean b = matcher04.find();
        String hash = b ? matcher04.group(1) : "";
        String name = b ? matcher04.group(2) : "";
        
        int multiplier = "KB".equals(suf) ? 1024 : "MB".equals(suf) ? 1024 * 1024 : "GB".equals(suf) ? 1024 * 1024 * 1024 : 0;
        long sizel = (long) (Float.valueOf(size) * multiplier);
        int c = Integer.parseInt(count0) + Integer.parseInt(count);
        return new Meta(c, ago, sizel, hash, name);
    }
    
    public static void walk(Path path) throws IOException {
//        Set<Entry> list = new HashSet<>();

        FileManager fm = FileManager.of("temp.ffffv");
        Files.walk(path).forEach(e -> {
            try {
                if (Files.isDirectory(e) == false) {
                    String lines = Files.readString(e);
                    String[] split = lines.split("\\.\\.\\.");
                    for (int i = 1; i < split.length; i++) {
                        Meta meta = parse(split[i]);
                        if (split.length < 10) {
                            continue;
                        }
                        MetaFile me = new MetaFile(meta.name, meta.size);
                        TorrentMeta name = new TorrentMeta(meta.name, List.of(me));
                        name.count = meta.filesCount;
//                        Entry entry = TorrentMeta.toEntry(meta.hash, name);
                        fm.saveMeta(meta.hash, name);
                        System.out.println(meta);
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
//        ObjectMapper name = new ObjectMapper();
//        String writeValueAsString = name.writeValueAsString(list);
//        Files.writeString(Path.of("xaxaxax"), writeValueAsString, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    static class Meta {
        int filesCount;
        String ago;
        long size;
        String hash;
        String name;
        public Meta(int filesCount, String ago, long size, String hash, String name) {
            super();
            this.filesCount = filesCount;
            this.ago = ago;
            this.size = size;
            this.hash = hash;
            this.name = name;
        }
        @Override
        public String toString() {
            return "Meta [hash=" + hash + ", name=" + name + ", filesCount=" + filesCount + ", ago=" + ago + ", size="
                    + size + "]";
        }
        
        
        
    }
    
    public static void main(String[] args) throws IOException {
//        Parser.walk(Path.of("C:\\Users\\7470\\Downloads\\btdig"));

        FileManager of = FileManager.of("xaxa");
        of.convert("torrents.txttemp.ffffv");

    }
}
