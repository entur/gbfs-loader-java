package org.entur.gbfs.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

  private HttpUtils() {}

  private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);
  private static final long TIMEOUT_CONNECTION = 5000;

  public static InputStream getData(URI uri) throws IOException {
    return getData(uri, null);
  }

  public static InputStream getData(String uri) throws IOException {
    return getData(URI.create(uri));
  }

  public static InputStream getData(
    URI uri,
    Long timeout,
    Map<String, String> requestHeaderValues
  ) throws IOException {
    HttpGet httpget = new HttpGet(uri);
    if (requestHeaderValues != null) {
      for (Map.Entry<String, String> entry : requestHeaderValues.entrySet()) {
        httpget.addHeader(entry.getKey(), entry.getValue());
      }
    }
    timeout = (timeout == null) ? TIMEOUT_CONNECTION : timeout;
    HttpClient httpclient = getClient(timeout);
    HttpResponse response = httpclient.execute(httpget);
    if (response.getStatusLine().getStatusCode() != 200) {
      LOG.warn("Got non-200 status code: {}", response.getStatusLine().getStatusCode());
      return null;
    }

    HttpEntity entity = response.getEntity();
    if (entity == null) {
      return null;
    }
    return entity.getContent();
  }

  public static InputStream getData(URI uri, Map<String, String> requestHeaderValues)
    throws IOException {
    return getData(uri, TIMEOUT_CONNECTION, requestHeaderValues);
  }

  private static HttpClient getClient(long timeoutSocket) {
    return HttpClientBuilder
      .create()
      .setRoutePlanner(new SystemDefaultRoutePlanner(null))
      .setDefaultSocketConfig(
        SocketConfig.custom().setSoTimeout((int) timeoutSocket).build()
      )
      .setDefaultRequestConfig(
        RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()
      )
      .build();
  }
}
