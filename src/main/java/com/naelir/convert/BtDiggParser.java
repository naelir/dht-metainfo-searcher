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

public class BtDiggParser {

    
    public static void main(String[] args) throws IOException {
        Path path = Path.of("C:\\Users\\7470\\Downloads\\btdig");

        FileManager fm = FileManager.of("llalal__");
        fm.convert("llalal__");

    }
}
