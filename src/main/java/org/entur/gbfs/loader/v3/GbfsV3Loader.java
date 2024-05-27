package org.entur.gbfs.loader.v3;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.loader.BaseGbfsLoader;
import org.entur.gbfs.loader.GbfsFeed;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSGbfs;

public class GbfsV3Loader extends BaseGbfsLoader<GBFSFeed.Name, GBFSGbfs> {

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   */
  public GbfsV3Loader(String url) {
    this(url, new HashMap<>());
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
   */
  public GbfsV3Loader(String url, Map<String, String> httpHeaders) {
    this(url, httpHeaders, null, null);
  }

  /**
   * Create a new GbfsLoader
   *
   * @param url The URL to the GBFS discovery file
   * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
   *                             each request
   */
  public GbfsV3Loader(String url, RequestAuthenticator requestAuthenticator) {
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
  public GbfsV3Loader(
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
  public GbfsV3Loader(
    String url,
    Map<String, String> httpHeaders,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    super(url, httpHeaders, requestAuthenticator, timeoutConnection, GBFSGbfs.class);
    init();
  }

  @Override
  protected List<GbfsFeed<GBFSFeed.Name, ?>> getFeeds() {
    List<GBFSFeed> feeds = getDiscoveryFeed().getData().getFeeds();

    return feeds
      .stream()
      .map(feed ->
        new GbfsFeed<>(
          feed.getName(),
          GBFSFeedName.implementingClass(feed.getName()),
          URI.create(feed.getUrl())
        )
      )
      .collect(Collectors.toList());
  }

  @Override
  protected GBFSFeed.Name getDiscoveryFeedName() {
    return GBFSFeed.Name.GBFS;
  }
}
