package com.naelir.convert;

import java.io.IOException;

import com.naelir.dht.FileManager;

public class Parser {
    
    public static void main(String[] args) throws IOException {
        FileManager of = FileManager.of();
        of.convert();
    }
}
