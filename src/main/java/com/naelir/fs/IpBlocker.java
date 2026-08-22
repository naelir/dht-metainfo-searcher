package com.naelir.fs;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class IpBlocker {

    private static final List<String> DENIED = Arrays.asList("AS", "OC", "AF", "LOCAL");
    private static final List<String> ALLOWED = Arrays.asList("EU");
    

    public static boolean denied(Pair<String, String> location) {
        return DENIED.contains(location.getLeft());
    }

    public static boolean allowed(Pair<String, String> location) {
        return ALLOWED.contains(location.getLeft());
    }

}
