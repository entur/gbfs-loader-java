package org.entur.gbfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import org.entur.gbfs.loader.v2.GbfsV2Delivery;
import org.entur.gbfs.loader.v3.GbfsV3Delivery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GBFSSubscriptionTest {

  private CountDownLatch waiter;

  @Test
  void testSubscription() throws URISyntaxException, InterruptedException {
    waiter = new CountDownLatch(1);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
    String subscriber = loader.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      getTestConsumer()
    );
    loader.update();
    waiter.await();
    loader.unsubscribe(subscriber);
  }

  @Test
  void testSubscriptionWithCustomThreadPool()
    throws URISyntaxException, InterruptedException {
    int parallellCount = 2;
    ForkJoinPool customThreadPool = new ForkJoinPool(parallellCount);
    waiter = new CountDownLatch(parallellCount);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager(customThreadPool);
    String id1 = loader.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      getTestConsumer()
    );
    String id2 = loader.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/helsinki/gbfs.json", "en"),
      getTestConsumer()
    );
    loader.update();
    waiter.await();
    loader.unsubscribe(id1);
    loader.unsubscribe(id2);
    customThreadPool.shutdown();
  }

  Consumer<GbfsV2Delivery> getTestConsumer() {
    return delivery -> {
      Assertions.assertNotNull(delivery);
      Assertions.assertEquals(
        0,
        delivery.validationResult().getSummary().getErrorsCount()
      );
      waiter.countDown();
    };
  }

  GbfsSubscriptionOptions getTestOptions(String url, String languageCode)
    throws URISyntaxException {
    GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
    options.setDiscoveryURI(new URI(url));
    options.setLanguageCode(languageCode);
    options.setEnableValidation(true);
    return options;
  }

  @Test
  void testV3Subscription() throws URISyntaxException, InterruptedException {
    waiter = new CountDownLatch(1);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
    String subscriber = loader.subscribeV3(
      getV3TestOptions("file:src/test/resources/gbfs/v3/getaroundstavanger/gbfs.json"),
      getV3TestConsumer()
    );
    loader.update();
    waiter.await();
    loader.unsubscribe(subscriber);
  }

  Consumer<GbfsV3Delivery> getV3TestConsumer() {
    return delivery -> {
      Assertions.assertNotNull(delivery);
      Assertions.assertEquals(
        0,
        delivery.validationResult().getSummary().getErrorsCount()
      );
      waiter.countDown();
    };
  }

  GbfsSubscriptionOptions getV3TestOptions(String url) throws URISyntaxException {
    GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
    options.setDiscoveryURI(new URI(url));
    options.setEnableValidation(true);
    return options;
  }
}
