package org.entur.gbfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.http.GBFSFeedUpdater;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.gbfs.GBFSFeed;
import org.entur.gbfs.v2_3.gbfs.GBFSFeedName;
import org.entur.gbfs.v2_3.gbfs.GBFSFeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according to individual feed's
 * TTL rules.
 */
public class GbfsLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsLoader.class);

  /** One updater per feed type(?)*/
  private final Map<GBFSFeedName, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();

  private final String url;

  private final Map<String, String> httpHeaders;

  private final String languageCode;

  private final RequestAuthenticator requestAuthenticator;

  private byte[] rawDiscoveryFileData;

  private GBFS disoveryFileData;

  private final AtomicBoolean setupComplete = new AtomicBoolean(false);

  private final Lock updateLock = new ReentrantLock();

  private final Long timeoutConnection;

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   */
  public GbfsLoader(String url) {
    this(url, new HashMap<>(), null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param languageCode The language code to be used to look up feeds in the discovery file
   */
  public GbfsLoader(String url, String languageCode) {
    this(url, new HashMap<>(), languageCode);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   * @param languageCode The language code to be used to look up feeds in the discovery file
   */
  public GbfsLoader(String url, Map<String, String> httpHeaders, String languageCode) {
    this(url, httpHeaders, languageCode, null, null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param languageCode The language code to be used to look up feeds in the discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *                             each request
   */
  public GbfsLoader(
    String url,
    String languageCode,
    RequestAuthenticator requestAuthenticator
  ) {
    this(url, new HashMap<>(), languageCode, requestAuthenticator, null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param languageCode The language code to be used to look up feeds in the discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *            each request.
   * @param timeoutConnection The timeout connection value.
   */
  public GbfsLoader(
    String url,
    String languageCode,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    this(url, new HashMap<>(), languageCode, requestAuthenticator, timeoutConnection);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   * @param languageCode The language code to be used to look up feeds in the discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *            each request.
   */
  public GbfsLoader(
    String url,
    Map<String, String> httpHeaders,
    String languageCode,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    if (requestAuthenticator == null) {
      this.requestAuthenticator = new DummyRequestAuthenticator();
    } else {
      this.requestAuthenticator = requestAuthenticator;
    }

    this.url = url;
    this.httpHeaders = httpHeaders;
    this.languageCode = languageCode;
    this.timeoutConnection = timeoutConnection;

    init();
  }

  public AtomicBoolean getSetupComplete() {
    return setupComplete;
  }

  private synchronized void init() {
    if (setupComplete.get()) {
      return;
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid url " + url);
    }

    if (!url.endsWith("gbfs.json")) {
      LOG.warn(
        "GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.",
        url
      );
    }

    var discoveryFileUpdater = new GBFSFeedUpdater<>(
      uri,
      requestAuthenticator,
      GBFSFeedName.GBFS.implementingClass(),
      httpHeaders,
      timeoutConnection
    );
    discoveryFileUpdater.fetchData();

    rawDiscoveryFileData = discoveryFileUpdater.getRawData();

    if (rawDiscoveryFileData != null) {
      disoveryFileData = (GBFS) discoveryFileUpdater.getData();
    }

    if (disoveryFileData != null) {
      createUpdaters();
      setupComplete.set(true);
    } else {
      LOG.warn("Could not fetch the feed auto-configuration file from {}", uri);
    }
  }

  private void createUpdaters() {
    // Pick first language if none defined
    GBFSFeeds feeds = languageCode == null
      ? disoveryFileData.getFeedsData().values().iterator().next()
      : disoveryFileData.getFeedsData().get(languageCode);
    if (feeds == null) {
      throw new RuntimeException(
        "Language " + languageCode + " does not exist in feed " + url
      );
    }

    // Create updater for each file
    for (GBFSFeed feed : feeds.getFeeds()) {
      GBFSFeedName feedName = feed.getName();
      if (feedUpdaters.containsKey(feedName)) {
        throw new RuntimeException(
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
            feed.getUrl(),
            requestAuthenticator,
            feed.getName().implementingClass(),
            httpHeaders,
            timeoutConnection
          )
        );
      }
    }
  }

  /**
   * Checks if any of the feeds should be updated base on the TTL and fetches. Returns true, if any feeds were updated.
   */
  public boolean update() {
    if (!setupComplete.get()) {
      init();
    }

    boolean didUpdate = false;
    if (updateLock.tryLock()) {
      try {
        for (GBFSFeedUpdater<?> updater : feedUpdaters.values()) {
          if (updater.shouldUpdate()) {
            updater.fetchData();
            // TODO didUpdate is set to true, once for one feed an update was initiated,
            // no matter if successful or not(?)
            didUpdate = true;
          }
        }
      } finally {
        // be sure to release lock, even in case an exception is thrown
        updateLock.unlock();
      }
    }
    return didUpdate;
  }

  public GBFS getDiscoveryFeed() {
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

  public byte[] getRawFeed(GBFSFeedName feedName) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(feedName);
    if (updater == null) {
      return null;
    }
    return updater.getRawData();
  }
}
