package org.entur.gbfs.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GBFSFeedUpdater<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GBFSFeedUpdater.class);
  public static final String GET_LAST_UPDATED = "getLastUpdated";

  /**
   * URL for the individual GBFS file
   */
  private final URI url;

  /**
   * To which class should the file be deserialized to
   */
  private final Class<T> implementingClass;

  private final RequestAuthenticator requestAuthenticator;

  private T data;
  private byte[] rawData;

  private final UpdateStrategy updateStrategy = new UpdateStrategy();

  private final Map<String, String> httpHeaders;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  private final Long timeout;

  public GBFSFeedUpdater(
    URI url,
    RequestAuthenticator requestAuthenticator,
    Class<T> implementingClass,
    Map<String, String> httpHeaders,
    Long timeout
  ) {
    this.url = url;
    this.requestAuthenticator = requestAuthenticator;
    this.implementingClass = implementingClass;
    this.httpHeaders = httpHeaders;
    this.timeout = timeout;
  }

  public URI getUrl() {
    return url;
  }

  public T getData() {
    return data;
  }

  public byte[] getRawData() {
    return rawData;
  }

  public void fetchData() {
    requestAuthenticator.authenticateRequest(httpHeaders);
    rawData = fetchFeed(url, httpHeaders);

    if (rawData == null) {
      LOG.warn("Invalid data for {}", url);
      updateStrategy.rescheduleAfterFailure();
      data = null;
      return;
    }

    try {
      data = objectMapper.readValue(rawData, implementingClass);
    } catch (IOException e) {
      LOG.warn("Error unmarshalling feed", e);
      updateStrategy.rescheduleAfterFailure();
      data = null;
      return;
    }

    try {
      // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
      // class, and have to use introspection to get the method references, as they do not share a supertype.

      Integer lastUpdated;

      if (
        implementingClass
          .getMethod(GET_LAST_UPDATED)
          .getReturnType()
          .equals(Integer.class)
      ) {
        lastUpdated =
          (Integer) implementingClass.getMethod(GET_LAST_UPDATED).invoke(data);
      } else {
        Date lastUpdatedDate = (Date) implementingClass
          .getMethod(GET_LAST_UPDATED)
          .invoke(data);
        lastUpdated = Math.toIntExact(lastUpdatedDate.getTime() / 1000);
      }

      Integer ttl = (Integer) implementingClass.getMethod("getTtl").invoke(data);
      updateStrategy.scheduleNextUpdate(lastUpdated, ttl);
    } catch (
      NoSuchMethodException
      | InvocationTargetException
      | IllegalAccessException
      | ClassCastException e
    ) {
      LOG.warn("Invalid data for {}", url);
      updateStrategy.rescheduleAfterFailure();
    }
  }

  public boolean shouldUpdate() {
    return updateStrategy.shouldUpdate();
  }

  private byte[] fetchFeed(URI uri, Map<String, String> httpHeaders) {
    String proto = uri.getScheme();

    if (proto.equals("http") || proto.equals("https")) {
      return fetchFeedFromHttp(uri, httpHeaders);
    } else {
      return fetchFeedFromFile(uri);
    }
  }

  private byte[] fetchFeedFromFile(URI uri) {
    try (InputStream is = uri.toURL().openStream()) {
      return is.readAllBytes();
    } catch (MalformedURLException e) {
      LOG.warn("Error reading GBFS feed from file due to malformed URL {}", uri, e);
      return null;
    } catch (IOException e) {
      LOG.warn("Error reading GBFS feed from file {}", uri, e);
      return null;
    }
  }

  private byte[] fetchFeedFromHttp(URI uri, Map<String, String> httpHeaders) {
    try (InputStream is = HttpUtils.getData(uri, timeout, httpHeaders)) {
      if (is == null) {
        LOG.warn("Failed to get data from url {}", uri);
        return null;
      }

      return is.readAllBytes();
    } catch (IOException e) {
      LOG.warn("Error (bad connection) reading GBFS feed from {}", uri, e);
      return null;
    }
  }
}
