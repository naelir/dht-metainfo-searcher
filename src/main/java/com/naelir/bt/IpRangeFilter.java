package com.naelir.bt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class IpRangeFilter {
    public static final String UNKNOWN = "Unknown";
    public static final String DEFAULT = "default";
    public static final Logger logger = LogManager.getLogger(IpRangeFilter.class);
    public static final List<IpRange> RANGES_ALLOW = getAllowRanges();
    public static final List<IpRange> RANGES_DENY = getDenyRanges();

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.DEBUG);
        boolean denied = isDenied(toBytes("77.70.34.83"));
        System.out.println(denied);
    }
    
    public static boolean isDenied(byte[] ip) {
        BigInteger address = toBigInteger(ip);
        for (IpRange ipRange : RANGES_DENY) {
            if (address.compareTo(ipRange.from) >= 0 && address.compareTo(ipRange.to) <= 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllow(byte[] ip) {
        BigInteger address = toBigInteger(ip);
        for (IpRange ipRange : RANGES_ALLOW) {
            if (address.compareTo(ipRange.from) >= 0 && address.compareTo(ipRange.to) <= 0) {
                return true;
            }
        }
        return false;
    }
    
    private static List<IpRange> getAllowRanges() {
        List<IpRange> list = new ArrayList<>();
        try (
                InputStream is = Files.newInputStream(Path.of("ip-range.txt"), StandardOpenOption.READ);
                InputStreamReader name = new InputStreamReader(is);
                BufferedReader e = new BufferedReader(name)
        ) {
            String line;
            String country = null;
            while ((line = e.readLine()) != null) {
                if (line.startsWith("#")) {
                    country = line.substring(1).trim();
                    continue;
                }
                String[] split = line.split("-");
                if (split.length < 2) {
                    continue;
                }
                list.add(new IpRange(split[0], split[1], country));
            }
        } catch (Exception e) {
            logger.error("Failed to read IP range file: {}", e.getMessage(), e);
        }
        return list;
    }

    private static List<IpRange> getDenyRanges() {
        List<IpRange> list = new ArrayList<>();
        try (
                InputStream is = Files.newInputStream(Path.of("ip-range.deny"), StandardOpenOption.READ);
                InputStreamReader name = new InputStreamReader(is);
                BufferedReader e = new BufferedReader(name)
        ) {
            String line;
            String country = null;
            while ((line = e.readLine()) != null) {
                if (line.startsWith("#")) {
                    country = line.substring(1).trim();
                    continue;
                }
                String[] split = line.split("-");
                if (split.length < 2) {
                    continue;
                }
                list.add(new IpRange(split[0], split[1], country));
            }
        } catch (Exception e) {
            logger.error("Failed to read IP range file: {}", e.getMessage(), e);
        }
        return list;
    }

    public static IpRange findRange(byte[] ip, List<IpRange> ranges) {
        BigInteger address = toBigInteger(ip);
        for (IpRange ipRange : ranges) {
            if (address.compareTo(ipRange.from) >= 0 && address.compareTo(ipRange.to) <= 0)
                return ipRange;
        }
        return null;
    }

    /**
     * Converts the raw byte array of an {@link InetAddress} to an unsigned
     * {@link BigInteger}. The byte array is always big-endian (most-significant
     * byte first), so prepending a zero byte guarantees that the result is treated
     * as a positive number regardless of the value of the most-significant bit.
     */
    static BigInteger toBigInteger(byte[] bytes) {
        // Prepend 0x00 so that BigInteger treats the value as unsigned.
        byte[] unsigned = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, unsigned, 1, bytes.length);
        return new BigInteger(unsigned);
    }

    /**
     * Converts a string IP address representation to its raw big-endian byte array.
     *
     * <p>
     * For IPv4 addresses (e.g. {@code "192.168.1.1"}) the returned array is 4 bytes
     * long. For IPv6 addresses (e.g. {@code "::1"}) it is 16 bytes long. The format
     * is the same as {@link InetAddress#getAddress()}: most-significant byte first.
     * </p>
     *
     * @param address a dotted-decimal IPv4 or colon-hex IPv6 string
     * @return the raw byte representation of the address
     * @throws IllegalArgumentException if {@code address} cannot be parsed as an IP
     *                                  address
     */
    static byte[] toBytes(String address) {
        try {
            return InetAddress.getByName(address).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + e.getMessage(), e);
        }
    }
}
