package org.entur.gbfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.entur.gbfs.authentication.Oauth2ClientCredentialsGrantRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.v2.GbfsV2Delivery;
import org.entur.gbfs.v2.GbfsV2Loader;
import org.entur.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class GbfsAuthenticationTest {

  private CountDownLatch waiter;

  private final String TEST_URL = "";
  private final String TEST_LANGUAGE_CODE = "";
  private final String TEST_TOKEN_URL = "";
  private final String TEST_CLIENT_ID = "";
  private final String TEST_CLIENT_PASSWORD = "";

  @Test
  @Disabled("Test code provided to test live Oauth2 authentication")
  void testOauth2ClientCredentialsGrant() {
    GbfsV2Loader loader = new GbfsV2Loader(
      TEST_URL,
      TEST_LANGUAGE_CODE,
      new Oauth2ClientCredentialsGrantRequestAuthenticator(
        URI.create(TEST_TOKEN_URL),
        TEST_CLIENT_ID,
        TEST_CLIENT_PASSWORD
      )
    );
    loader.update();
    GBFSFreeBikeStatus feed = loader.getFeed(GBFSFreeBikeStatus.class);
    Assertions.assertNotNull(feed.getData().getBikes().get(0).getBikeId());
  }

  @Test
  @Disabled("Test code provided to test live Oauth2 authentication")
  void testOauth2ClientCredentialsGrantWithSubscription()
    throws URISyntaxException, InterruptedException {
    waiter = new CountDownLatch(1);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
    RequestAuthenticator requestAuthenticator =
      new Oauth2ClientCredentialsGrantRequestAuthenticator(
        URI.create(TEST_TOKEN_URL),
        TEST_CLIENT_ID,
        TEST_CLIENT_PASSWORD
      );
    String subscriber = loader.subscribeV2(
      getTestOptions(TEST_URL, TEST_LANGUAGE_CODE, requestAuthenticator),
      getTestConsumer()
    );
    loader.update();
    waiter.await();
    loader.unsubscribe(subscriber);
  }

  GbfsSubscriptionOptions getTestOptions(
    String url,
    String languageCode,
    RequestAuthenticator requestAuthenticator
  ) throws URISyntaxException {
    GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
    options.setDiscoveryURI(new URI(url));
    options.setLanguageCode(languageCode);
    options.setRequestAuthenticator(requestAuthenticator);
    return options;
  }

  Consumer<GbfsV2Delivery> getTestConsumer() {
    return delivery -> {
      Assertions.assertNotNull(delivery);
      waiter.countDown();
    };
  }
}
