package com.naelir.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTPS client backed by Apache HttpComponents 4.5.14.
 * <p>
 * Supports GET and POST requests and returns the response body as a
 * {@link String}. Two modes are available:
 * <ul>
 * <li><b>Trust-all</b> (default) – accepts any server certificate; useful for
 * internal/test endpoints.</li>
 * <li><b>Default SSL</b> – uses the JVM default trust store for proper
 * certificate validation.</li>
 * </ul>
 * The client is closeable and should be used inside a try-with-resources block
 * or closed explicitly after use.
 */
public class HttpsClient implements AutoCloseable {
    private static final Logger LOG = LogManager.getLogger(HttpsClient.class);
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_CONN_REQUEST_TIMEOUT_MS = 5_000;
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * Builds an {@link SSLContext}.
     *
     * @param trustAll when {@code true} a trust-all strategy is used; otherwise the
     *                 JVM default trust store is used
     */
    private static SSLContext buildSslContext(boolean trustAll)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        if (trustAll) {
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
            return SSLContextBuilder.create().loadTrustMaterial(null, acceptingTrustStrategy).build();
        }
        return SSLContext.getDefault();
    }
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
//    public static void main(String[] args) throws HttpsClientException, IOException, InterruptedException {
//        List<String> list = List.of(
//                /*
//                 * "https://predb.me/?cats=movies-hd&page=",
//                 * "https://predb.me/?cats=tv-hd&page=",
//                 */
//                "https://predb.me/?cats=music&page=", "https://predb.me/?cats=games&page=");
//        Path path = Paths.get(System.getProperty("user.home")).resolve("predb.me");
//        try (
//                HttpsClient name = new HttpsClient();
//                BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
//                        StandardOpenOption.APPEND)
//        ) {
//            for (String string : list) {
//                for (int i = 1; i < 200; i++) {
//                    String body = name.get(string.concat(Integer.toString(i)));
//                    bufferedWriter.append(body);
//                    bufferedWriter.newLine();
//                    Thread.sleep(2000);
//                    System.out.println(i);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    // https://zamunda.rip/api/torrents?q=&bg_audio=true&bg_movies=false&bg_arena=false&zelka=false&offset=80

    static String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
//
//    public static void main(String[] args) throws HttpsClientException, IOException, InterruptedException {
//        List<String> list = List.of(
////                "https://www.limetorrents.fun/browse-torrents/Movies/date/"//,
////                "https://www.limetorrents.fun/browse-torrents/TV-shows/date/"//,
//                "https://www.limetorrents.fun/browse-torrents/Games/date/"
//                );
//        Path path = Paths.get(System.getProperty("user.home")).resolve(RandomStringUtils.randomAlphanumeric(10));
//        try (
//                HttpsClient name = new HttpsClient();
//                BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
//                        StandardOpenOption.APPEND)
//        ) {
//            for (String string : list) {
//                for (int i = 380; i < 1000; i++) {
//                    String body = name.get(string.concat(Integer.toString(i)));
//                    bufferedWriter.append(body);
//                    bufferedWriter.newLine();
//                    bufferedWriter.flush();
//                    Thread.sleep(1000);
//                    System.out.println(i);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws HttpsClientException, IOException, InterruptedException {
        List<String> list = List.of(
                "https://zamunda.rip/api/torrents?q=&bg_audio=true&bg_movies=false&bg_arena=false&zelka=false&offset=");
        Path path = Paths.get(System.getProperty("user.home")).resolve(RandomStringUtils.randomAlphanumeric(10));
        try (
                HttpsClient name = new HttpsClient();
                BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            for (String string : list) {
                for (int i = 0; i < 450000; i = i + 20) {
                    String body = name.get(string.concat(Integer.toString(i)));
                    bufferedWriter.append(body);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    Thread.sleep(1000);
                    System.out.println(i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final CloseableHttpClient mHttpClient;
    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    private String user;
    private String password;

    /**
     * Creates an HttpsClient that trusts <b>all</b> server certificates.
     *
     * @throws HttpsClientException if the SSL context cannot be built
     */
    public HttpsClient() throws HttpsClientException {
        this(true, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_SOCKET_TIMEOUT_MS, DEFAULT_CONN_REQUEST_TIMEOUT_MS);
    }

    /**
     * Creates an HttpsClient with full control over trust and timeouts.
     *
     * @param trustAll             {@code true} to disable certificate validation
     * @param connectTimeoutMs     connection timeout in milliseconds
     * @param socketTimeoutMs      socket (read) timeout in milliseconds
     * @param connRequestTimeoutMs connection-from-pool request timeout in
     *                             milliseconds
     * @throws HttpsClientException if the SSL context cannot be built
     */
    public HttpsClient(boolean trustAll, int connectTimeoutMs, int socketTimeoutMs, int connRequestTimeoutMs)
            throws HttpsClientException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .setConnectionRequestTimeout(connRequestTimeoutMs)
                .build();
        try {
            SSLContext sslContext = buildSslContext(trustAll);
            this.mHttpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new HttpsClientException("Failed to build SSL context", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        this.mHttpClient.close();
    }

    public HttpsClient credentials(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }

    /**
     * Extracts the response body as a UTF-8 string and validates the status code.
     *
     * @param response HTTP response (will not be closed here)
     * @param url      request URL used for error messages
     * @return response body string; empty string when there is no body
     * @throws HttpsClientException when the server returns a non-2xx status
     * @throws IOException          on I/O errors while reading the body
     */
    private String extractBody(HttpResponse response, String url) throws HttpsClientException, IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            LOG.debug("header {}, {}", header.getName(), header.getValue());
        }
        String body = (entity != null) ? EntityUtils.toString(entity, DEFAULT_CHARSET) : "";
        if (statusCode < 200 || statusCode >= 300)
            throw new HttpsClientException(
                    "Request to [" + url + "] returned non-success status: " + statusCode + " – " + body);
        LOG.debug("Received response [{}] from: {}", statusCode, url);
        return body;
    }

    /**
     * Sends an HTTP GET request and returns the response body as a string.
     *
     * @param url target URL (must start with {@code https://})
     * @return response body; never {@code null} (empty string when there is no
     *         body)
     * @throws HttpsClientException on any I/O or HTTP-level error
     */
    public String get(String url) throws HttpsClientException {
        return get(url, null);
    }
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sends an HTTP GET request with custom headers and returns the response body
     * as a string.
     *
     * @param url     target URL
     * @param headers request headers, may be {@code null}
     * @return response body; never {@code null}
     * @throws HttpsClientException on any I/O or HTTP-level error
     */
    public String get(String url, Map<String, String> headers) throws HttpsClientException {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
//        request.addHeader("Authentication", createBasicAuthHeader(this.user, this.password));
        LOG.debug("Sending GET request to: {}", url);
        Header[] allHeaders = request.getAllHeaders();
        for (Header header : allHeaders) {
            LOG.warn(header);
        }
        try (
                CloseableHttpResponse response = this.mHttpClient.execute(request)
        ) {
            return extractBody(response, url);
        } catch (IOException e) {
            throw new HttpsClientException("GET request failed for URL: " + url, e);
        }
    }

    /**
     * Sends an HTTP POST request with a string body and returns the response body
     * as a string.
     *
     * @param url         target URL
     * @param requestBody request body content; may be {@code null}
     * @param contentType MIME type of the request body (e.g.
     *                    {@code "application/json"})
     * @return response body; never {@code null}
     * @throws HttpsClientException on any I/O or HTTP-level error
     */
    public String post(String url, String requestBody, String contentType) throws HttpsClientException {
        return post(url, requestBody, contentType, null);
    }
    // -------------------------------------------------------------------------
    // Nested exception type
    // -------------------------------------------------------------------------

    /**
     * Sends an HTTP POST request with a string body and custom headers and returns
     * the response body as a string.
     *
     * @param url         target URL
     * @param requestBody request body content; may be {@code null}
     * @param contentType MIME type of the request body
     * @param headers     additional request headers; may be {@code null}
     * @return response body; never {@code null}
     * @throws HttpsClientException on any I/O or HTTP-level error
     */
    public String post(String url, String requestBody, String contentType, Map<String, String> headers)
            throws HttpsClientException {
        HttpPost request = new HttpPost(url);
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
        request.addHeader("Authentication", createBasicAuthHeader(this.user, this.password));
        if (requestBody != null) {
            try {
                StringEntity entity = new StringEntity(requestBody, DEFAULT_CHARSET);
                entity.setContentType(contentType);
                request.setEntity(entity);
            } catch (Exception e) {
                throw new HttpsClientException("Failed to create request entity for URL: " + url, e);
            }
        }
        LOG.debug("Sending POST request to: {}", url);
        try (
                CloseableHttpResponse response = this.mHttpClient.execute(request)
        ) {
            return extractBody(response, url);
        } catch (IOException e) {
            throw new HttpsClientException("POST request failed for URL: " + url, e);
        }
    }

    /**
     * Checked exception thrown by {@link HttpsClient} on failure.
     */
    public static class HttpsClientException extends Exception {
        private static final long serialVersionUID = 1L;

        public HttpsClientException(String message) {
            super(message);
        }

        public HttpsClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}