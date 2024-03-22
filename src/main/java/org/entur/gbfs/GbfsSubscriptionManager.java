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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import org.entur.gbfs.v2.GbfsV2Delivery;
import org.entur.gbfs.v2.GbfsV2Subscription;
import org.entur.gbfs.v3.GbfsV3Delivery;
import org.entur.gbfs.v3.GbfsV3Subscription;

/**
 * Manage a set of subscriptions for different GBFS feeds.
 * A subscription consumes atomic updates of a set of GBFS files belonging to
 * a single system.
 * The subscription manager has subscription methods for v2 and v3 GBFS feeds.
 */
public class GbfsSubscriptionManager {

  private final Map<String, GbfsSubscription> subscriptions = new ConcurrentHashMap<>();

  private ForkJoinPool customThreadPool;

  public GbfsSubscriptionManager() {}

  public GbfsSubscriptionManager(ForkJoinPool customThreadPool) {
    this.customThreadPool = customThreadPool;
  }

  /**
   * Start a subscription on a GBFS v2.x feed
   *
   * Since v2.x is backwards-compatible with v1.x, v1.x feeds can also be
   * consumed with this subscription.
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
   * Update all subscriptions
   */
  public void update() {
    Optional
      .ofNullable(customThreadPool)
      .orElse(ForkJoinPool.commonPool())
      .execute(() ->
        subscriptions.values().parallelStream().forEach(GbfsSubscription::update)
      );
  }

  /**
   * Stop a subscription on a GBFS feed
   *
   * @param identifier An identifier returned by subscribe method.
   */
  public void unsubscribe(String identifier) {
    subscriptions.remove(identifier);
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
