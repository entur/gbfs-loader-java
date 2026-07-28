/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.gbfs;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.entur.gbfs.loader.GbfsSubscription;
import org.entur.gbfs.loader.v2.GbfsV2Delivery;
import org.entur.gbfs.loader.v2.GbfsV2Subscription;
import org.entur.gbfs.loader.v3.GbfsV3Delivery;
import org.entur.gbfs.loader.v3.GbfsV3Subscription;

/**
 * Manage a set of subscriptions for different GBFS feeds.
 * A subscription consumes atomic updates of a set of GBFS files belonging to
 * a single system.
 * The subscription manager has subscription methods for v2 and v3 GBFS feeds.
 */
public class GbfsSubscriptionManager {

  private final Map<String, GbfsSubscription> subscriptions = new ConcurrentHashMap<>();

  private ForkJoinPool customThreadPool;

  // Default timeout for unsubscribe wait
  private static final long DEFAULT_UNSUBSCRIBE_TIMEOUT_MS = 30_000; // 30 seconds

  public GbfsSubscriptionManager() {}

  public GbfsSubscriptionManager(ForkJoinPool customThreadPool) {
    this.customThreadPool = customThreadPool;
  }

  /**
   * Start a subscription on a GBFS v2.x feed
   * <p>
   * Since v2.x is backwards-compatible with v1.x, v1.x feeds can also be
   * consumed with this subscription.
   * </p>
   *
   * @param options Options
   * @param consumer A consumer that will handle receiving updates from the loader
   * @return A string identifier
   */
  public String subscribeV2(
    GbfsSubscriptionOptions options,
    Consumer<GbfsV2Delivery> consumer
  ) {
    return subscribe(new GbfsV2Subscription(options, consumer));
  }

  /**
   * Start a subscription on a GBFS v2.x feed
   * <p>
   * Since v2.x is backwards-compatible with v1.x, v1.x feeds can also be
   * consumed with this subscription.
   * </p>
   *
   * @param options Options
   * @param consumer A consumer that will handle receiving updates from the loader
   * @param updateInterceptor A subscription update interceptor
   * @return A string identifier
   */
  public String subscribeV2(
    GbfsSubscriptionOptions options,
    Consumer<GbfsV2Delivery> consumer,
    SubscriptionUpdateInterceptor updateInterceptor
  ) {
    return subscribe(new GbfsV2Subscription(options, consumer, updateInterceptor));
  }

  /**
   * Start a subscription on a GBFS v3.x feed
   *
   * @param options Options
   * @param consumer A consumer that will handle receiving updates from the loader}
   * @return A string identifier
   */
  public String subscribeV3(
    GbfsSubscriptionOptions options,
    Consumer<GbfsV3Delivery> consumer
  ) {
    return subscribe(new GbfsV3Subscription(options, consumer));
  }

  /**
   * Start a subscription on a GBFS v3.x feed
   *
   * @param options Options
   * @param consumer A consumer that will handle receiving updates from the loader}
   * @param updateInterceptor A subscription update interceptor
   * @return A string identifier
   */
  public String subscribeV3(
    GbfsSubscriptionOptions options,
    Consumer<GbfsV3Delivery> consumer,
    SubscriptionUpdateInterceptor updateInterceptor
  ) {
    return subscribe(new GbfsV3Subscription(options, consumer, updateInterceptor));
  }

  /**
   * Update all subscriptions
   */
  public void update() {
    subscriptions.values().parallelStream().forEach(subscription -> update(subscription));
  }

  /**
   * Update single subscription
   *
   * @param identifier Identifier of subscription
   */
  public void update(String identifier) {
    update(subscriptions.get(identifier));
  }

  /**
   * Update single subscription
   *
   * @param subscription Subscription which should be updated
   */
  private void update(GbfsSubscription subscription) {
    // Create the future BEFORE scheduling to avoid race condition where
    // unsubscribe is called before the async task starts executing.
    // This ensures getCurrentUpdate() will see it even if task hasn't started yet.
    CompletableFuture<Void> updateFuture = new CompletableFuture<>();
    subscription.setCurrentUpdate(updateFuture);

    Optional
      .ofNullable(customThreadPool)
      .orElse(ForkJoinPool.commonPool())
      .execute(subscription::update);
  }

  /**
   * Stop a subscription on a GBFS feed asynchronously.
   * Returns a CompletableFuture that completes when any in-flight update finishes.
   * Uses the default timeout of 30 seconds.
   *
   * @param identifier An identifier returned by subscribe method
   * @return CompletableFuture that completes when it's safe to clean up caches
   */
  public CompletableFuture<Void> unsubscribeAsync(String identifier) {
    return unsubscribeAsync(
      identifier,
      DEFAULT_UNSUBSCRIBE_TIMEOUT_MS,
      TimeUnit.MILLISECONDS
    );
  }

  /**
   * Stop a subscription on a GBFS feed asynchronously with a custom timeout.
   * Returns a CompletableFuture that completes when any in-flight update finishes.
   * The future will complete exceptionally with TimeoutException if the timeout expires.
   *
   * @param identifier An identifier returned by subscribe method
   * @param timeout Maximum time to wait for in-flight updates
   * @param unit Time unit for the timeout
   * @return CompletableFuture that completes when it's safe to clean up caches
   */
  public CompletableFuture<Void> unsubscribeAsync(
    String identifier,
    long timeout,
    TimeUnit unit
  ) {
    // Remove from subscriptions map first to prevent new updates
    GbfsSubscription subscription = subscriptions.remove(identifier);

    if (subscription == null) {
      // Already unsubscribed - return completed future
      return CompletableFuture.completedFuture(null);
    }

    // Get the subscription's current update future and apply timeout
    // This is the key insight: no polling needed, just wait on the subscription's future!
    return subscription.getCurrentUpdate().orTimeout(timeout, unit);
  }

  /**
   * Stop a subscription on a GBFS feed (blocking version).
   * This method blocks until the subscription's in-flight update completes.
   *
   * @param identifier An identifier returned by subscribe method
   * @deprecated Use {@link #unsubscribeAsync(String)} for better async composition
   */
  @Deprecated
  public void unsubscribe(String identifier) {
    try {
      unsubscribeAsync(identifier).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while unsubscribing", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Error while unsubscribing", e);
    }
  }

  private String subscribe(GbfsSubscription subscription) {
    String id = UUID.randomUUID().toString();

    subscription.init();

    // Only add subscription if setup is complete
    if (subscription.getSetupComplete()) {
      subscriptions.put(id, subscription);
      return id;
    }

    return null;
  }
}
