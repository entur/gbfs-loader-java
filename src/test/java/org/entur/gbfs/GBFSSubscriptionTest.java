package org.entur.gbfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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

  @Test
  void testUnsubscribeAsyncWaitsForInFlightUpdate()
    throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    CountDownLatch updateStarted = new CountDownLatch(1);
    CountDownLatch updateCanFinish = new CountDownLatch(1);
    AtomicBoolean updateCompleted = new AtomicBoolean(false);

    Consumer<GbfsV2Delivery> slowConsumer = delivery -> {
      updateStarted.countDown();
      try {
        updateCanFinish.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      updateCompleted.set(true);
    };

    GbfsSubscriptionManager manager = new GbfsSubscriptionManager();
    String id = manager.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      slowConsumer
    );
    manager.update(id);

    // Wait for update to start
    Assertions.assertTrue(updateStarted.await(5, TimeUnit.SECONDS));

    // Start unsubscribe - get the future
    CompletableFuture<Void> unsubscribeFuture = manager.unsubscribeAsync(id);

    // Future should not be done yet
    Thread.sleep(500);
    Assertions.assertFalse(unsubscribeFuture.isDone());

    // Allow update to complete
    updateCanFinish.countDown();

    // Future should now complete
    unsubscribeFuture.get(5, TimeUnit.SECONDS);
    Assertions.assertTrue(updateCompleted.get());
  }

  @Test
  void testUnsubscribeAsyncTimeoutWithStuckUpdate()
    throws URISyntaxException, InterruptedException {
    Consumer<GbfsV2Delivery> stuckConsumer = delivery -> {
      try {
        Thread.sleep(60_000); // Sleep longer than timeout
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };

    GbfsSubscriptionManager manager = new GbfsSubscriptionManager();
    String id = manager.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      stuckConsumer
    );
    manager.update(id);

    Thread.sleep(500); // Let update start

    // Should timeout
    CompletableFuture<Void> future = manager.unsubscribeAsync(id, 1, TimeUnit.SECONDS);

    Assertions.assertThrows(
      TimeoutException.class,
      () -> {
        try {
          future.get();
        } catch (ExecutionException e) {
          throw e.getCause();
        }
      }
    );
  }

  @Test
  void testUnsubscribeAsyncWithNoInFlightUpdates()
    throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    GbfsSubscriptionManager manager = new GbfsSubscriptionManager();
    String id = manager.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      delivery -> {}
    );

    // Unsubscribe immediately (no updates in progress)
    CompletableFuture<Void> future = manager.unsubscribeAsync(id);

    // Should complete immediately
    Assertions.assertTrue(future.isDone());
    future.get(100, TimeUnit.MILLISECONDS); // Should not throw
  }

  @Test
  void testUnsubscribeAsyncAlreadyUnsubscribed()
    throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    GbfsSubscriptionManager manager = new GbfsSubscriptionManager();
    String id = manager.subscribeV2(
      getTestOptions("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json", "nb"),
      delivery -> {}
    );

    // Unsubscribe once
    manager.unsubscribeAsync(id).get();

    // Unsubscribe again - should return completed future
    CompletableFuture<Void> future = manager.unsubscribeAsync(id);
    Assertions.assertTrue(future.isDone());
    future.get(100, TimeUnit.MILLISECONDS); // Should not throw
  }
}
