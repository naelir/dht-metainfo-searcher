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
    private static final List<String> GAME_KEYWORDS = List.of("FitGirl");
    private static final List<String> XXX = List.of("xxx");
    public static final Pattern TV = Pattern.compile("\\.S\\d+E\\d+\\.");
    private static final Pattern MUSIC = Pattern.compile("\\([a-zA-Z]+\\d+\\)");
    private static final Pattern VALID_NAMES = Pattern.compile("[\\[\\]\\-_()\\.\\da-zA-Z]+");

    public static Genre from(String name, List<MetaFile> list) {
        if (name == null)
            return Genre.UNKNOWN;
        String lower = name.toLowerCase();
        if (matchKeyword(lower, XXX))
            return Genre.XXX;
        else if (TV.matcher(lower).find())
            return Genre.TV;
        else if (MUSIC.matcher(lower).find())
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
        else if (matchKeyword(lower, GAME_KEYWORDS))
            return Genre.GAME_REPACK;
        return Genre.UNKNOWN;
    }

    public static boolean match(String name, boolean checkDash) {
        boolean isOk = VALID_NAMES.matcher(name).matches();
        if (checkDash) {
            boolean haveDash = name.indexOf("-") > 0;
            return isOk && haveDash;
        } else
            return isOk;
    }
    
    public static boolean fine(TorrentMeta meta, boolean checkDash) {
        String name = meta.getName();
        if (Genre.XXX.equals(meta.genre) || Genre.UNKNOWN.equals(meta.genre)) {
            return false;
        }
        boolean isOk = VALID_NAMES.matcher(name).matches();
        if (checkDash) {
            boolean haveDash = name.indexOf("-") > 0;
            return isOk && haveDash;
        } else
            return isOk;
    }

    public static boolean match(TorrentMeta meta) {
        if (StringUtils.isBlank(meta.getName()))
            return false;
        boolean nameMatch = match(meta.getName(), true);
        boolean nfo = false;
        for (MetaFile metaFile : meta.getList()) {
            if (meta.genre.equals(Genre.TV) || meta.genre.equals(Genre.MOVIE_VIDEO)) {
                nfo = true;
                continue;
            }
            if (metaFile.path != null && metaFile.path.indexOf(".nfo") > 0) {
                nfo = match(metaFile.path, false);
                break;
            }
        }
        return nameMatch && nfo;
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
