package org.entur.gbfs.v3;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.entur.gbfs.BaseGbfsLoader;
import org.entur.gbfs.DuplicateFeedException;
import org.entur.gbfs.InvalidURLException;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.http.GBFSFeedUpdater;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeed;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeedName;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsLoader extends BaseGbfsLoader<GBFSFeed.Name> {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsLoader.class);

  private final String url;

  private final Map<String, String> httpHeaders;

  private final RequestAuthenticator requestAuthenticator;

  private GBFSGbfs disoveryFileData;

  private final Long timeoutConnection;

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   */
  public GbfsLoader(String url) {
    this(url, new HashMap<>());
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   */
  public GbfsLoader(String url, Map<String, String> httpHeaders) {
    this(url, httpHeaders, null, null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *                             each request
   */
  public GbfsLoader(String url, RequestAuthenticator requestAuthenticator) {
    this(url, new HashMap<>(), requestAuthenticator, null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *            each request.
   * @param timeoutConnection The timeout connection value.
   */
  public GbfsLoader(
    String url,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    this(url, new HashMap<>(), requestAuthenticator, timeoutConnection);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *            each request.
   */
  public GbfsLoader(
    String url,
    Map<String, String> httpHeaders,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    this.requestAuthenticator =
      Objects.requireNonNullElseGet(requestAuthenticator, DummyRequestAuthenticator::new);
    this.url = url;
    this.httpHeaders = httpHeaders;
    this.timeoutConnection = timeoutConnection;

    init();
  }

  public synchronized void init() {
    byte[] rawDiscoveryFileData;
    if (getSetupComplete().get()) {
      return;
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new InvalidURLException("Invalid url " + url);
    }

    var discoveryFileUpdater = new GBFSFeedUpdater<>(
      uri,
      requestAuthenticator,
      GBFSGbfs.class,
      httpHeaders,
      timeoutConnection
    );
    discoveryFileUpdater.fetchData();

    rawDiscoveryFileData = discoveryFileUpdater.getRawData();

    if (rawDiscoveryFileData != null) {
      disoveryFileData = discoveryFileUpdater.getData();
    }

    if (disoveryFileData != null) {
      createUpdaters();
      getSetupComplete().set(true);
    } else {
      LOG.warn("Could not fetch the feed auto-configuration file from {}", uri);
    }
  }

  private void createUpdaters() {
    List<GBFSFeed> feeds = disoveryFileData.getData().getFeeds();

    // Create updater for each file
    for (GBFSFeed feed : feeds) {
      GBFSFeed.Name feedName = feed.getName();
      if (feedUpdaters.containsKey(feedName)) {
        throw new DuplicateFeedException(
          "Feed contains duplicate url for feed " +
          feedName +
          ". " +
          "Urls: " +
          feed.getUrl() +
          ", " +
          feedUpdaters.get(feedName).getUrl()
        );
      }

      // name is null, if the file is of unknown type, skip those
      if (feed.getName() != null) {
        feedUpdaters.put(
          feedName,
          new GBFSFeedUpdater<>(
            URI.create(feed.getUrl()),
            requestAuthenticator,
            GBFSFeedName.implementingClass(feedName),
            httpHeaders,
            timeoutConnection
          )
        );
      }
    }
  }

  public GBFSGbfs getDiscoveryFeed() {
    return disoveryFileData;
  }

  /**
   * Gets the most recent contents of the feed, which contains an object of type T.
   */
  public <T> T getFeed(Class<T> feed) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(GBFSFeedName.fromClass(feed));
    if (updater == null) {
      return null;
    }
    return feed.cast(updater.getData());
  }

  public Optional<byte[]> getRawFeed(GBFSFeed.Name feedName) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(feedName);
    if (updater == null) {
      return Optional.empty();
    }
    return Optional.of(updater.getRawData());
  }
}
