package org.entur.gbfs.loader;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.http.GBFSFeedUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseGbfsLoader<S, T> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseGbfsLoader.class);
  private final AtomicBoolean setupComplete = new AtomicBoolean(false);
  private final Lock updateLock = new ReentrantLock();
  private final Map<S, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();
  private final Map<Class<?>, S> classMap = new HashMap<>();
  private T disoveryFileData;
  private final GBFSFeedUpdater<T> discoveryFileUpdater;
  private final Map<String, String> httpHeaders;
  private final RequestAuthenticator requestAuthenticator;
  private final Long timeoutConnection;

  protected BaseGbfsLoader(
    String discoveryUrl,
    Map<String, String> httpHeaders,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection,
    Class<T> discoveryFileClass
  ) {
    this.requestAuthenticator =
      Objects.requireNonNullElseGet(requestAuthenticator, DummyRequestAuthenticator::new);
    this.httpHeaders = httpHeaders;
    this.timeoutConnection = timeoutConnection;
    this.discoveryFileUpdater =
      new GBFSFeedUpdater<>(
        URI.create(discoveryUrl),
        this.requestAuthenticator,
        discoveryFileClass,
        httpHeaders,
        timeoutConnection
      );
  }

  public synchronized void init() {
    if (setupComplete.get()) {
      return;
    }

    byte[] rawDiscoveryFileData;

    discoveryFileUpdater.fetchData();

    rawDiscoveryFileData = discoveryFileUpdater.getRawData();

    if (rawDiscoveryFileData != null) {
      disoveryFileData = discoveryFileUpdater.getData();
    }

    if (disoveryFileData != null) {
      createUpdaters();
      setupComplete.set(true);
    } else {
      LOG.warn(
        "Could not fetch the feed auto-configuration file from {}",
        discoveryFileUpdater.getUrl()
      );
    }
  }

  public boolean getSetupComplete() {
    return setupComplete.get();
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
            if (updater.getData() != null) {
              didUpdate = true;
            }
          }
        }
      } finally {
        // be sure to release lock, even in case an exception is thrown
        updateLock.unlock();
      }
    }
    return didUpdate;
  }

  private void createUpdaters() {
    // Create updater for each file
    for (GbfsFeed<S, ?> feed : getFeeds()) {
      S feedName = feed.name();
      if (feedUpdaters.containsKey(feedName)) {
        throw new DuplicateFeedException(
          "Feed contains duplicate url for feed " +
          feedName +
          ". " +
          "Urls: " +
          feed.uri() +
          ", " +
          feedUpdaters.get(feedName).getUrl()
        );
      }

      // name is null, if the file is of unknown type, skip those
      if (feedName != null) {
        feedUpdaters.put(
          feedName,
          new GBFSFeedUpdater<>(
            feed.uri(),
            requestAuthenticator,
            feed.implementingClass(),
            httpHeaders,
            timeoutConnection
          )
        );
        classMap.put(feed.implementingClass(), feedName);
      }
    }
  }

  protected abstract List<GbfsFeed<S, ?>> getFeeds();

  public T getDiscoveryFeed() {
    return disoveryFileData;
  }

  /**
   * Gets the most recent contents of the feed, which contains an object of type T.
   */
  public <R> R getFeed(Class<R> feed) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(classMap.get(feed));
    if (updater == null) {
      return null;
    }
    return feed.cast(updater.getData());
  }

  public byte[] getRawFeed(S feedName) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(feedName);
    if (updater == null) {
      return null;
    }
    return updater.getRawData();
  }
}
