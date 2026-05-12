package com.naelir.bt;

import java.math.BigInteger;

public class IpRange {
    String representation;
    BigInteger from;
    BigInteger to;
    public String country;

    public IpRange(String from, String to, String country) {
        this.country = country;
        this.representation = from + to;
        this.from = IpRangeFilter.toBigInteger(IpRangeFilter.toBytes(from));
        this.to = IpRangeFilter.toBigInteger(IpRangeFilter.toBytes(to));
    }

    @Override
    public String toString() {
        return "IpRange [representation=" + representation + ", country=" + country + "]";
    }
    
    
}
