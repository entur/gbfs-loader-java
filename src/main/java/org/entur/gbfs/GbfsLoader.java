package org.entur.gbfs;

import java.util.Map;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.loader.v2.GbfsV2Loader;
import org.entur.gbfs.loader.v3.GbfsV3Loader;

/**
 * This class provides direct access to loaders for v2 and v3.
 */
public class GbfsLoader {

  /**
   * Loader for GBFS v2 feeds
   * @param url
   * @param httpHeaders
   * @param languageCode
   * @param requestAuthenticator
   * @param timeoutConnection
   * @return
   */
  public GbfsV2Loader gbfsV2Loader(
    String url,
    Map<String, String> httpHeaders,
    String languageCode,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    return new GbfsV2Loader(
      url,
      httpHeaders,
      languageCode,
      requestAuthenticator,
      timeoutConnection
    );
  }

  /**
   * Loader for GBFS v3 feeds
   * @param url
   * @param httpHeaders
   * @param requestAuthenticator
   * @param timeoutConnection
   * @return
   */
  public GbfsV3Loader gbfsV3Loader(
    String url,
    Map<String, String> httpHeaders,
    RequestAuthenticator requestAuthenticator,
    Long timeoutConnection
  ) {
    return new GbfsV3Loader(url, httpHeaders, requestAuthenticator, timeoutConnection);
  }
}
