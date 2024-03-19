package org.entur.gbfs;

import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.v2.GbfsV2Loader;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.gbfs.GBFSFeedName;
import org.entur.gbfs.v3.GbfsV3Loader;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according to individual feed's
 * TTL rules.
 */
public class GbfsLoader implements GbfsVersionLoader {
  private final GbfsVersionLoader gbfsVersionLoader;


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
    this(url, httpHeaders, languageCode, requestAuthenticator, timeoutConnection, Version.V2);
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
          Long timeoutConnection,
          Version version
  ) {
    if (version.equals(Version.V3)) {
      gbfsVersionLoader = new GbfsV3Loader(
              url,
              httpHeaders,
              languageCode,
              requestAuthenticator,
              timeoutConnection
      );
    } else {
      gbfsVersionLoader = new GbfsV2Loader(
              url,
              httpHeaders,
              languageCode,
              requestAuthenticator,
              timeoutConnection
      );
    }
  }

  @Override
  public AtomicBoolean getSetupComplete() {
    return gbfsVersionLoader.getSetupComplete();
  }

  @Override
  public boolean update() {
    return gbfsVersionLoader.update();
  }

  @Override
  public GBFS getDiscoveryFeed() {
    return gbfsVersionLoader.getDiscoveryFeed();
  }

  @Override
  public GBFSGbfs getV3DiscoveryFeed() {
    return gbfsVersionLoader.getV3DiscoveryFeed();
  }

  @Override
  public <T> T getFeed(Class<T> feed) {
    return gbfsVersionLoader.getFeed(feed);
  }

  @Override
  public byte[] getRawFeed(GBFSFeedName feedName) {
    return gbfsVersionLoader.getRawFeed(feedName);
  }

  public byte[] getRawV3Feed(org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeedName feedName) {
    return gbfsVersionLoader.getRawV3Feed(feedName);
  }
}
