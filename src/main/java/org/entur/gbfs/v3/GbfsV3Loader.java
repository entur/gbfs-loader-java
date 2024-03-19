package org.entur.gbfs.v3;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.entur.gbfs.GbfsVersionLoader;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.http.GBFSFeedUpdater;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeed;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeedName;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsV3Loader implements GbfsVersionLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsV3Loader.class);

  /** One updater per feed type(?)*/
  private final Map<GBFSFeed.Name, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();

  private final String url;

  private final Map<String, String> httpHeaders;

  private final String languageCode;

  private final RequestAuthenticator requestAuthenticator;

  private GBFSGbfs disoveryFileData;

  private final AtomicBoolean setupComplete = new AtomicBoolean(false);

  private final Lock updateLock = new ReentrantLock();

  private final Long timeoutConnection;

  public GbfsV3Loader(
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

  public synchronized void init() {
    byte[] rawDiscoveryFileData;
    if (setupComplete.get()) {
      return;
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid url " + url);
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
      setupComplete.set(true);
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

  @Override
  public GBFSGbfs getV3DiscoveryFeed() {
    return disoveryFileData;
  }

  @Override
  public GBFS getDiscoveryFeed() {
    // unsupported
    return null;
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

  @Override
  public byte[] getRawFeed(org.entur.gbfs.v2_3.gbfs.GBFSFeedName feedName) {
    // unsupported
    return new byte[0];
  }

  @Override
  public byte[] getRawV3Feed(GBFSFeedName feedName) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(feedName);
    if (updater == null) {
      return null;
    }
    return updater.getRawData();
  }
}
