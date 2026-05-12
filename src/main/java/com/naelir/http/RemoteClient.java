package com.naelir.http;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naelir.bt.Entry;
import com.naelir.bt.TorrentMeta;

public class RemoteClient implements IRemoteClient {
    
    public static final String REMOTE_URL = "http://localhost:8000/api/entries";
    
    private String url;

    public RemoteClient(String url) {
        this.url = url;
    }
    
    public static final Logger logger = LogManager.getLogger(RemoteClient.class);

    @Override
    public void saveMeta(String hash, TorrentMeta meta) {
        try {
            Entry entry = TorrentMeta.toEntry(hash, meta);
            ObjectMapper name = new ObjectMapper();
            String json = name.writeValueAsString(entry);
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);
            CloseableHttpClient minimal = HttpClients.createMinimal();
            minimal.execute(httpPost);
            minimal.close();
        } catch (IOException e) {
            logger.error("Failed to save metadata for hash {}: {}", hash, e.getMessage());
        }
    }

}
