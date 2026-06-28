package com.naelir.convert;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.naelir.dht.FileManager;

public class Parser {

    private static final Pattern FILE_COUNT = Pattern.compile(".+?&nbsp;(\\d+) hidden");
    private static final Pattern AGO = Pattern.compile("found (.+?)<.+");
    private static final Pattern SIZE = Pattern.compile("([\\d\\.]+)&nbsp;([MBG]+)");
    private static final Pattern HASH_NAME = Pattern.compile("urn:btih:(.{40}).+?dn=(.+?)&");
    
    public static Meta parse(String line) {
        Matcher matcher01 = FILE_COUNT.matcher(line);
        Matcher matcher02 = AGO.matcher(line);
        Matcher matcher03 = SIZE.matcher(line);
        Matcher matcher04 = HASH_NAME.matcher(line);
        
        String count = matcher01.matches() ? matcher01.group(1) : "1";
        String ago = matcher02.matches() ? matcher02.group(1) : "";
        String size = matcher03.matches() ? matcher03.group(1).concat(matcher03.group(2)) : "";
        String hash = matcher04.matches() ? matcher04.group(1) : "";
        String name = matcher04.matches() ? matcher04.group(2) : "";
        
        return new Meta(count, ago, size, hash, name);
    }
    
    public static void walk(Path path) throws IOException {
        List<Meta> list = new ArrayList<Parser.Meta>();
        Files.walk(path).forEach(e -> {
            try {
                if (Files.isDirectory(e) == false) {
                    String lines = Files.readString(e);
                    String[] split = lines.split("\\.\\.\\.");
                    for (int i = 1; i < split.length; i++) {
                        Meta meta = parse(split[i]);
                        list.add(meta);
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        for (Meta meta : list) {
            System.out.println(meta);
        }
    }
    
    static class Meta {
        String filesCount;
        String ago;
        String size;
        String hash;
        String name;
        public Meta(String filesCount, String ago, String size, String hash, String name) {
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
        String a = "</a></div></div></div></div></div></div><div style=display:table-row><div style=padding-top:15px></div></div><div class=one_result style=display:table-row;background-color:#e8e8e8><div style=display:table-cell;color:rgb(0,0,0)><div style=display:table><div style=display:table-row><div class=torrent_name style=display:table-cell><a style=color:rgb(0,0,204);text-decoration:underline;font-size:150% href=\"https://btdig.com/b8a953c33ad19226baa9c7171a09f256a18fb5d5/=afo\"><b style=color:red;background-color:yellow>afo</b>-tslomw-0301-720-web[EZTVx.to].mkv</a></div></div></div><div style=display:table><div style=display:table-row><div style=display:table-cell><span class=torrent_size style=color:#666;padding-left:10px>1.47&nbsp;GB</span><span class=torrent_age style=\"color:rgb(0,102,0);padding-left:10px;margin:0px 4px\">found 3 months ago</span></div></div></div><div class=torrent_excerpt style=display:table;padding:10px;white-space:nowrap><div class=\"fa fa-file-video-o\" style=padding-left:0em>&nbsp;<b style=color:red;background-color:yellow>afo</b>-tslomw-0301-720-web[EZTVx.to].mkv</div><span style=color:#666;padding-left:10px>&nbsp;1.47&nbsp;GB\r\n"
                + "</span><br></div><div style=display:table;width:100%;padding:10px><div style=display:table-row><div class=torrent_magnet style=display:table-cell><div class=\"fa fa-magnet\" style=color:#cc0000><a href=\"magnet:?xt=urn:btih:b8a953c33ad19226baa9c7171a09f256a18fb5d5&amp;dn=afo-tslomw-0301-720-web%5BEZTVx.to%5D.mkv&amp;tr=udp://tracker.openbittorrent.com:80&amp;tr=udp://tracker.opentrackr.org:1337/announce\" title=\"Download via magnet-link\">&nbsp;magnet:?xt=urn:btih:b8a953c33a";
        Meta meta = Parser.parse(a);
        System.out.println(meta);
    }
}
