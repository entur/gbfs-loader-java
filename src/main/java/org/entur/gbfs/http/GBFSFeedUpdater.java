package org.entur.gbfs.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.jetbrains.annotations.NotNull;
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
  private byte[] rawData = null;

  private final UpdateStrategy updateStrategy;

  private final GBFSHttpClient httpClient;

  private Map<String, String> httpHeaders = new HashMap<>();

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  private final Long timeout;

  public GBFSFeedUpdater(
    @NotNull URI url,
    @NotNull RequestAuthenticator requestAuthenticator,
    @NotNull Class<T> implementingClass,
    Map<String, String> httpHeaders,
    Long timeout
  ) {
    this(
      url,
      requestAuthenticator,
      implementingClass,
      httpHeaders,
      timeout,
      new GBFSHttpClient(),
      new UpdateStrategy()
    );
  }

  protected GBFSFeedUpdater(
    @NotNull URI url,
    @NotNull RequestAuthenticator requestAuthenticator,
    @NotNull Class<T> implementingClass,
    Map<String, String> httpHeaders,
    Long timeout,
    @NotNull GBFSHttpClient httpClient,
    @NotNull UpdateStrategy updateStrategy
  ) {
    this.url = url;
    this.requestAuthenticator = requestAuthenticator;
    this.implementingClass = implementingClass;
    if (httpHeaders != null) {
      this.httpHeaders = httpHeaders;
    }
    this.timeout = timeout;
    this.httpClient = httpClient;
    this.updateStrategy = updateStrategy;
  }

  public URI getUrl() {
    return url;
  }

  public T getData() {
    return data;
  }

  public Optional<byte[]> getRawData() {
    return Optional.ofNullable(rawData);
  }

  public boolean fetchOnce() {
    requestAuthenticator.authenticateRequest(httpHeaders);
    rawData = fetchFeed(url, httpHeaders).orElse(null);

    if (!validateRawData(rawData)) {
      return false;
    }

    return deserializeData(rawData);
  }

  public boolean update() {
    if (!shouldUpdate()) {
      return false;
    }

    requestAuthenticator.authenticateRequest(httpHeaders);
    rawData = fetchFeed(url, httpHeaders).orElse(null);

    if (!validateRawData(rawData)) {
      updateStrategy.rescheduleAfterFailure();
      return false;
    }

    if (!deserializeData(rawData)) {
      updateStrategy.rescheduleAfterFailure();
      return false;
    }

    if (!scheduleNextUpdate()) {
      updateStrategy.rescheduleAfterFailure();
      return false;
    }

    return true;
  }

  private boolean shouldUpdate() {
    return updateStrategy.shouldUpdate();
  }

  private Optional<byte[]> fetchFeed(URI uri, Map<String, String> httpHeaders) {
    String proto = uri.getScheme();

    if (proto.equals("http") || proto.equals("https")) {
      return fetchFeedFromHttp(uri, httpHeaders);
    } else {
      return fetchFeedFromFile(uri);
    }
  }

  private Optional<byte[]> fetchFeedFromFile(URI uri) {
    try (InputStream is = uri.toURL().openStream()) {
      return Optional.of(is.readAllBytes());
    } catch (MalformedURLException e) {
      LOG.warn("Error reading GBFS feed from file due to malformed URL {}", uri, e);
      return Optional.empty();
    } catch (IOException e) {
      LOG.warn("Error reading GBFS feed from file {}", uri, e);
      return Optional.empty();
    }
  }

  private Optional<byte[]> fetchFeedFromHttp(URI uri, Map<String, String> httpHeaders) {
    try (InputStream is = httpClient.getData(uri, timeout, httpHeaders)) {
      if (is == null) {
        LOG.warn("Failed to get data from url {}", uri);
        return Optional.empty();
      }

      return Optional.of(is.readAllBytes());
    } catch (IOException e) {
      LOG.warn("Error (bad connection) reading GBFS feed from {}", uri, e);
      return Optional.empty();
    }
  }

  private boolean validateRawData(byte[] rawData) {
    if (rawData == null) {
      LOG.warn("Invalid data for {}", url);
      data = null;
      return false;
    }

    return true;
  }

  private boolean deserializeData(byte[] rawData) {
    try {
      data = objectMapper.readValue(rawData, implementingClass);
    } catch (IOException e) {
      LOG.warn("Error unmarshalling feed", e);
      data = null;
    }
    return data != null;
  }

  private boolean scheduleNextUpdate() {
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
      return true;
    } catch (
      NoSuchMethodException
      | InvocationTargetException
      | IllegalAccessException
      | ClassCastException
      | NullPointerException e
    ) {
      LOG.warn("Invalid data for {}", url);
      return false;
    }
  }
}
