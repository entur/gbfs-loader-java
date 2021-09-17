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

import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.gbfs.GBFS;
import org.entur.gbfs.v2_2.gbfs_versions.GBFSGbfsVersions;
import org.entur.gbfs.v2_2.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_2.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_2.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_2.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_2.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleTypes;

import java.util.function.Consumer;

public class GBFSSubscriber {
    private final GBFSLoaderOptions loaderOptions;
    private final Consumer<GBFSFeedDelivery> consumer;
    private GbfsFeedLoader feedLoader;

    public GBFSSubscriber(GBFSLoaderOptions loaderOptions, Consumer<GBFSFeedDelivery> consumer) {
        this.loaderOptions = loaderOptions;
        this.consumer = consumer;
    }

    public void start() {
        feedLoader = new GbfsFeedLoader(
                loaderOptions.discoveryURI.toString(),
                loaderOptions.headers,
                loaderOptions.languageCode
        );
    }

    public void update() {
        if (feedLoader.update()) {
            var delivery = new GBFSFeedDelivery();
            delivery.setDiscovery(feedLoader.getFeed(GBFS.class));
            delivery.setVersion(feedLoader.getFeed(GBFSGbfsVersions.class));
            delivery.setSystemInformation(feedLoader.getFeed(GBFSSystemInformation.class));
            delivery.setVehicleTypes(feedLoader.getFeed(GBFSVehicleTypes.class));
            delivery.setSystemRegions(feedLoader.getFeed(GBFSSystemRegions.class));
            delivery.setStationInformation(feedLoader.getFeed(GBFSStationInformation.class));
            delivery.setStationStatus(feedLoader.getFeed(GBFSStationStatus.class));
            delivery.setFreeBikeStatus(feedLoader.getFeed(GBFSFreeBikeStatus.class));
            delivery.setSystemAlerts(feedLoader.getFeed(GBFSSystemAlerts.class));
            delivery.setSystemCalendar(feedLoader.getFeed(GBFSSystemCalendar.class));
            delivery.setSystemHours(feedLoader.getFeed(GBFSSystemHours.class));
            delivery.setSystemPricingPlans(feedLoader.getFeed(GBFSSystemPricingPlans.class));
            delivery.setGeofencingZones(feedLoader.getFeed(GBFSGeofencingZones.class));

            consumer.accept(delivery);
        }
    }
}
