package org.entur.gbfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
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
  void testSubscriptionUpdateInterceptor()
    throws URISyntaxException, InterruptedException {
    waiter = new CountDownLatch(3);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
    String subscriber = loader.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      getTestConsumer(),
      new SubscriptionUpdateInterceptor() {
        @Override
        public void beforeUpdate() {
          waiter.countDown();
        }

        @Override
        public void afterUpdate() {
          waiter.countDown();
        }
      }
    );
    loader.update();
    Assertions.assertTrue(waiter.await(1, TimeUnit.SECONDS));
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
      Assertions.assertEquals(0, delivery.validationResult().summary().errorsCount());
      waiter.countDown();
    };
  }

  GbfsSubscriptionOptions getTestOptions(String url, String languageCode)
    throws URISyntaxException {
    return new GbfsSubscriptionOptions(
      new URI(url),
      languageCode,
      null,
      null,
      null,
      null,
      true
    );
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

  @Test
  void testV3SubscriptionUpdateInterceptor()
    throws URISyntaxException, InterruptedException {
    waiter = new CountDownLatch(3);
    GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
    String subscriber = loader.subscribeV3(
      getV3TestOptions("file:src/test/resources/gbfs/v3/getaroundstavanger/gbfs.json"),
      getV3TestConsumer(),
      new SubscriptionUpdateInterceptor() {
        @Override
        public void beforeUpdate() {
          waiter.countDown();
        }

        @Override
        public void afterUpdate() {
          waiter.countDown();
        }
      }
    );
    loader.update();
    Assertions.assertTrue(waiter.await(1, TimeUnit.SECONDS));
    loader.unsubscribe(subscriber);
  }

  Consumer<GbfsV3Delivery> getV3TestConsumer() {
    return delivery -> {
      Assertions.assertNotNull(delivery);
      Assertions.assertEquals(0, delivery.validationResult().summary().errorsCount());
      waiter.countDown();
    };
  }

  GbfsSubscriptionOptions getV3TestOptions(String url) throws URISyntaxException {
    return new GbfsSubscriptionOptions(new URI(url), null, null, null, null, null, true);
  }
}
