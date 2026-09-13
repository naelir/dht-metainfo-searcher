package com.naelir.fs;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class IpBlocker {

    private static final List<String> DENIED = Arrays.asList("AS", "AF", "LOCAL", "OC");
    private static final List<String> ALLOWED = Arrays.asList("EU");
    
    private static final List<String> DENIED_EXCEPTIONS = Arrays.asList("TR", "AU");
    private static final List<String> DENIED_ADDITIONS = Arrays.asList("RU");


    public static boolean denied(Pair<String, String> location) {
        return DENIED_EXCEPTIONS.contains(location.getRight()) == false
                && (DENIED.contains(location.getLeft()) || DENIED_ADDITIONS.contains(location.getRight()));
    }

    public static boolean allowed(Pair<String, String> location) {
        return ALLOWED.contains(location.getLeft());
    }
}
