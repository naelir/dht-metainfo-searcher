package com.naelir.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.naelir.dht.Generator;

public class FileLocationDb implements ILocationDb {
    private static final Logger logger = LogManager.getLogger(FileLocationDb.class);

    private static final Path HOME = Paths.get(System.getProperty("user.home")).resolve("dht-meta");
    private static final Path BASE_DIR = HOME.resolve("locationdb");
    private static final String SEP = ",";


    public static final ILocationDb INSTANCE = new FileLocationDb();
    
    @Override
    public Pair<String, String> location(byte[] ip) {
        String ipAddr = Generator.ip(ip);
        long ipLong = ipToLong(ipAddr);
        String prefix1 = ipAddr.substring(0, ipAddr.indexOf("."));
        Path shard = shardPath(prefix1);
        if (!Files.exists(shard)) {
            logger.info("cannot geolocate ip {}, internal one", ipAddr);
            return ImmutablePair.of("LOCAL", "LOCAL");
        }
        try (BufferedReader reader = Files.newBufferedReader(shard)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(SEP);
                if (parts.length < 6)
                    continue;
                String cidr = parts[0].trim();
                int slash = cidr.indexOf('/');
                if (slash < 0)
                    continue;
                long networkAddr = ipToLong(cidr.substring(0, slash));
                int prefixLen = Integer.parseInt(cidr.substring(slash + 1));
                long mask = prefixLen == 0 ? 0L : (0xFFFFFFFFL << (32 - prefixLen)) & 0xFFFFFFFFL;
                if ((ipLong & mask) == (networkAddr & mask)) {
                    return new ImmutablePair<String, String>(parts[2].trim(), parts[5].trim());
                }
            }
        } catch (IOException e) {
            logger.error("cannot determine country for ip {}", ipAddr, e);
        }
        logger.info("ip {} cannot be geolocated", ipAddr);

        return ImmutablePair.of("LOCAL", "LOCAL");
    }

    private static long ipToLong(String ipAddr) {
        String[] octets = ipAddr.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Long.parseLong(octets[i]) & 0xFF);
        }
        return result;
    }

    private static Path shardPath(String prefix) {
        return BASE_DIR.resolve(prefix.toUpperCase() + ".txt");
    }
    
}
