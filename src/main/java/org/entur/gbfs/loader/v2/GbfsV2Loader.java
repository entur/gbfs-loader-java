package org.entur.gbfs.loader.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.loader.BaseGbfsLoader;
import org.entur.gbfs.loader.GbfsFeed;
import org.entur.gbfs.loader.LanguageNotInFeedException;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFS;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeeds;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according to individual feed's
 * TTL rules.
 */
public class GbfsV2Loader extends BaseGbfsLoader<GBFSFeedName, GBFS> {

  private final String languageCode;

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   */
  public GbfsV2Loader(String url) {
    this(url, new HashMap<>(), null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param languageCode The language code to be used to look up feeds in the discovery file
   */
  public GbfsV2Loader(String url, String languageCode) {
    this(url, new HashMap<>(), languageCode);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   * @param languageCode The language code to be used to look up feeds in the discovery file
   */
  public GbfsV2Loader(String url, Map<String, String> httpHeaders, String languageCode) {
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
  public GbfsV2Loader(
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
  public GbfsV2Loader(
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
  public GbfsV2Loader(
    String url,
    Map<String, String> httpHeaders,
    String languageCode,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    super(url, httpHeaders, requestAuthenticator, timeoutConnection, GBFS.class);
    this.languageCode = languageCode;
    init();
  }

  @Override
  protected List<GbfsFeed<GBFSFeedName, ?>> getFeeds() {
    // Pick first language if none defined
    GBFSFeeds feeds = languageCode == null
      ? getDiscoveryFeed().getFeedsData().values().iterator().next()
      : getDiscoveryFeed().getFeedsData().get(languageCode);
    if (feeds == null) {
      throw new LanguageNotInFeedException(
        "Language " + languageCode + " does not exist in feed"
      );
    }

    return feeds
      .getFeeds()
      .stream()
      .map(feed ->
        new GbfsFeed<>(feed.getName(), feed.getName().implementingClass(), feed.getUrl())
      )
      .collect(Collectors.toList());
  }

  @Override
  protected GBFSFeedName getDiscoveryFeedName() {
    return GBFSFeedName.GBFS;
  }
}
