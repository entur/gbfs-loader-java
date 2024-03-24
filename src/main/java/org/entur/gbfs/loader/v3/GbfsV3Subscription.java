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

package org.entur.gbfs.loader.v3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.entur.gbfs.GbfsSubscriptionOptions;
import org.entur.gbfs.loader.GbfsSubscription;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeed;
import org.entur.gbfs.v3_0_RC2.gbfs_versions.GBFSGbfsVersions;
import org.entur.gbfs.v3_0_RC2.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v3_0_RC2.station_information.GBFSStationInformation;
import org.entur.gbfs.v3_0_RC2.station_status.GBFSStationStatus;
import org.entur.gbfs.v3_0_RC2.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v3_0_RC2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v3_0_RC2.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v3_0_RC2.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v3_0_RC2.vehicle_status.GBFSVehicleStatus;
import org.entur.gbfs.v3_0_RC2.vehicle_types.GBFSVehicleTypes;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.ValidationResult;

/**
 * Class to represent a subscription to GBFS feeds for a single system
 */
public class GbfsV3Subscription implements GbfsSubscription {

  private final GbfsSubscriptionOptions subscriptionOptions;
  private final Consumer<GbfsV3Delivery> consumer;
  private GbfsV3Loader loader;

  public GbfsV3Subscription(
    GbfsSubscriptionOptions subscriptionOptions,
    Consumer<GbfsV3Delivery> consumer
  ) {
    this.subscriptionOptions = subscriptionOptions;
    this.consumer = consumer;
  }

  /**
   * Initialize the subscription by creating a loader
   */
  public void init() {
    loader =
      new GbfsV3Loader(
        subscriptionOptions.discoveryURI().toString(),
        subscriptionOptions.headers(),
        subscriptionOptions.requestAuthenticator(),
        subscriptionOptions.timeout()
      );
  }

  /**
   * Check if the subscription is ready to use
   * @return True if the subscription setup is complete
   */
  public boolean getSetupComplete() {
    return loader.getSetupComplete();
  }

  /**
   * Update the subscription by updating the loader and push a new delivery
   * to the consumer if the update had changes
   */
  public void update() {
    if (loader.update()) {
      GbfsV3Delivery delivery = new GbfsV3Delivery(
        loader.getDiscoveryFeed(),
        loader.getFeed(GBFSGbfsVersions.class),
        loader.getFeed(GBFSSystemInformation.class),
        loader.getFeed(GBFSVehicleTypes.class),
        loader.getFeed(GBFSStationInformation.class),
        loader.getFeed(GBFSStationStatus.class),
        loader.getFeed(GBFSVehicleStatus.class),
        loader.getFeed(GBFSSystemRegions.class),
        loader.getFeed(GBFSSystemPricingPlans.class),
        loader.getFeed(GBFSSystemAlerts.class),
        loader.getFeed(GBFSGeofencingZones.class),
        subscriptionOptions.enableValidation() ? validateFeeds() : null
      );
      consumer.accept(delivery);
    }
  }

  private ValidationResult validateFeeds() {
    Map<String, InputStream> feeds = new HashMap<>();
    Arrays
      .stream(GBFSFeed.Name.values())
      .forEach(feedName -> {
        byte[] rawFeed = loader.getRawFeed(feedName);
        if (rawFeed != null) {
          feeds.put(feedName.value(), new ByteArrayInputStream(rawFeed));
        }
      });
    GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
    return validator.validate(feeds);
  }
}
