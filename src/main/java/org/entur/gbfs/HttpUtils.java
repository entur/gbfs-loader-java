package org.entur.gbfs;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);
    private static final long TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    public static InputStream getData(URI uri) throws IOException {
        return getData(uri, null);
    }

    public static InputStream getData(String uri) throws IOException {
        return getData(URI.create(uri));
    }

    public static InputStream getData(String uri, Map<String, String> headers) throws IOException {
        return getData(URI.create(uri), headers);
    }

    public static InputStream getData(
            URI uri, Long timeout, Map<String, String> requestHeaderValues
    ) throws IOException {
        HttpGet httpget = new HttpGet(uri);
        if (requestHeaderValues != null) {
            for (Map.Entry<String, String> entry : requestHeaderValues.entrySet()) {
                httpget.addHeader(entry.getKey(), entry.getValue());
            }
        }
        timeout = (timeout == null) ? TIMEOUT_CONNECTION : timeout;
        HttpClient httpclient = getClient(timeout, timeout);
        HttpResponse response = httpclient.execute(httpget);
        if(response.getStatusLine().getStatusCode() != 200) {
            LOG.warn("Got non-200 status code: {}", response.getStatusLine().getStatusCode());
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    public static InputStream getData(URI uri, Map<String, String> requestHeaderValues) throws IOException {
        return getData(uri, TIMEOUT_CONNECTION, requestHeaderValues);
    }

    public static void testUrl(String url) throws IOException {
        HttpHead head = new HttpHead(url);
        HttpClient httpclient = getClient();
        HttpResponse response = httpclient.execute(head);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() == 404) {
            throw new FileNotFoundException();
        }

        if (status.getStatusCode() != 200) {
            throw new RuntimeException("Could not get URL: " + status.getStatusCode() + ": "
                    + status.getReasonPhrase());
        }
    }
    
    private static HttpClient getClient() {
        return getClient(TIMEOUT_CONNECTION, TIMEOUT_SOCKET);
    }

    private static HttpClient getClient(long timeoutConnection, long timeoutSocket) {
        return HttpClientBuilder.create()
                .setRoutePlanner(new SystemDefaultRoutePlanner(null))
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout((int)timeoutSocket).build())
                .setConnectionTimeToLive(timeoutConnection, TimeUnit.MILLISECONDS)
                .build();
    }
}
