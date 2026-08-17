package com.naelir.bt;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.naelir.bt.TorrentMeta.Genre;
import com.naelir.bt.TorrentMeta.MetaFile;
 
public class NameFilter {
    private static final List<String> MOVIE_KEYWORDS = List.of("bluray", "x264", "x265", "h264", "h265", "dvdrip",
            "bdrip", "hdrip", "web-dl", "webrip", "webdl", "dvdscr", "cam", "hdcam", "hdts", "hdtv", "dvdr", "dvd5",
            "dvd9", "bgaudio");
    private static final List<String> GAME_REPACK_KEYWORDS = List.of("fitgirl");
    private static final List<String> ANIME_KEYWORDS = List.of("-toonshub", "-varyg", "-tsundere-raws", "-skyanime", "-uranime");
    private static final List<String> XXX = List.of("xxx", "jav", "worldmkv");
    private static final List<Genre> DENIED_GENRES = List.of(Genre.UNKNOWN, Genre.XXX, Genre.TVEP);
    public static final Pattern TV_SERIES = Pattern.compile("\\.S\\d+E\\d+\\.");
    public static final Pattern TV_SEASON = Pattern.compile("\\.S\\d\\d\\.[^E]");
    // do not allow group to start with DL, because it is not the real group name, but a part of WEB-DL
    private static final Pattern GROUP = Pattern.compile("(?!DL)[\\d\\[\\]a-zA-Z]+");

    private static final Pattern MUSIC = Pattern.compile("\\([A-Z]+\\d+\\)");
    private static final Pattern VALID_NAMES = Pattern.compile("[\\[\\]\\-_()\\.\\da-zA-Z]+");

    public static Genre from(String name, List<MetaFile> list) {
        if (name == null)
            return Genre.UNKNOWN;
        String lower = StringUtils.lowerCase(name);
        if (matchKeyword(lower, XXX))
            return Genre.XXX;
        else if (matchKeyword(lower, ANIME_KEYWORDS))
            return Genre.ANIME;
        else if (TV_SERIES.matcher(name).find())
            return Genre.TVEP;
        else if (TV_SEASON.matcher(name).find())
            return Genre.TV;
        else if (MUSIC.matcher(name).find())
            return Genre.MUSIC;
        else if (lower.indexOf(".ps5-") >= 0 || lower.indexOf(".psx-") >= 0 || lower.indexOf(".ps4-") >= 0)
            return Genre.GAME_PLAYSTATION;
        else if (lower.indexOf("_nsw-") >= 0)
            return Genre.GAME_NINTENDO;
        else if (lower.indexOf("_xbox360-") >= 0 || lower.indexOf("_xbox-") >= 0 || lower.indexOf("_xbox_") >= 0)
            return Genre.GAME_XBOX;
        else if (lower.indexOf("incl.key") >= 0)
            return Genre.SOFTWARE;
        else if (matchKeyword(lower, MOVIE_KEYWORDS))
            return Genre.MOVIE_VIDEO;
        else if (matchGameKeyword(list))
            return Genre.GAME_PC;
        else if (matchKeyword(lower, GAME_REPACK_KEYWORDS))
            return Genre.GAME_REPACK;
        return Genre.UNKNOWN;
    }

    public static boolean match(String name) {
        boolean isOk = VALID_NAMES.matcher(name).matches();
        if (isOk == false) {
            return false;
        }
        boolean haveDash = name.indexOf("-") > 0;
        if (haveDash == false) {
            return false;
        }
        String group = name.substring(name.lastIndexOf("-") + 1, name.length()).replace(".mkv", "");

        return GROUP.matcher(group).matches();
    }

    public static boolean fineMatch(String name) {
        if (matchKeyword(name.toLowerCase(), XXX))
            return false;
        return match(name);
    }
    
    public static boolean fine(TorrentMeta meta) {
        String name = meta.getName();
        if (DENIED_GENRES.contains(meta.genre)) {
            return false;
        }
        return match(name);
    }

    static boolean matchGameKeyword(List<MetaFile> list) {
        boolean hasIso = false;
        boolean hasNfo = false;
        for (MetaFile e : list) {
            if (e.path.endsWith(".iso"))
                hasIso = true;
            if (e.path.endsWith(".nfo"))
                hasNfo = true;
        }
        return hasIso && hasNfo;
    }

    static boolean matchKeyword(String name, List<String> keys) {
        String lower = name.toLowerCase();
        for (String keyword : keys) {
            if (lower.indexOf(keyword) >= 0)
                return true;
        }
        return false;
    }
}
