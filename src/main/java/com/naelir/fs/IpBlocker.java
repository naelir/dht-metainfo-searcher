package com.naelir.fs;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class IpBlocker {

    private static final List<String> DENIED = Arrays.asList("AS", "AF", "LOCAL");
    private static final List<String> ALLOWED = Arrays.asList("EU");
    
    private static final List<String> DENIED_EXCEPTIONS = Arrays.asList("Türkiye");


    public static boolean denied(Pair<String, String> location) {
        return DENIED.contains(location.getLeft()) && DENIED_EXCEPTIONS.contains(location.getRight()) == false;
    }

    public static boolean allowed(Pair<String, String> location) {
        return ALLOWED.contains(location.getLeft());
    }

}
