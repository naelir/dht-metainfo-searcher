package com.naelir.fs;

import org.apache.commons.lang3.tuple.Pair;

import com.naelir.dht.Node;

public interface ILocationDb {

    Pair<String, String> location(byte[] ip);

}