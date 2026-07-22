package com.naelir.tracker;

/**
 * UDP tracker protocol action codes (BEP-15).
 */
public enum TrackerAction {
    CONNECT(0),
    ANNOUNCE(1),
    SCRAPE(2),
    ERROR(3);

    public final int code;

    TrackerAction(int code) {
        this.code = code;
    }

    public static TrackerAction of(int code) {
        return switch (code) {
            case 0 -> CONNECT;
            case 1 -> ANNOUNCE;
            case 2 -> SCRAPE;
            case 3 -> ERROR;
            default -> throw new IllegalArgumentException("Unknown tracker action: " + code);
        };
    }
}
