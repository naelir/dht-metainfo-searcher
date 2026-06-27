package com.naelir.http;

import com.naelir.bt.TorrentMeta;

public interface IRemoteClient {

    void saveMeta(String hash, TorrentMeta meta);

}