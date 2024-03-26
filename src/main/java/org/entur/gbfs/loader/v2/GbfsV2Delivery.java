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

package org.entur.gbfs.loader.v2;

import org.entur.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.gbfs_versions.GBFSGbfsVersions;
import org.entur.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_3.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_3.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_3.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_3.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_3.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_3.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_3.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_3.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_3.vehicle_types.GBFSVehicleTypes;
import org.entur.gbfs.validation.model.ValidationResult;

/**
 * This class holds the data for all the GBFS files
 */
public record GbfsV2Delivery(
  GBFS discovery,
  GBFSGbfsVersions version,
  GBFSSystemInformation systemInformation,
  GBFSVehicleTypes vehicleTypes,
  GBFSStationInformation stationInformation,
  GBFSStationStatus stationStatus,
  GBFSFreeBikeStatus freeBikeStatus,
  GBFSSystemHours systemHours,
  GBFSSystemCalendar systemCalendar,
  GBFSSystemRegions systemRegions,
  GBFSSystemPricingPlans systemPricingPlans,
  GBFSSystemAlerts systemAlerts,
  GBFSGeofencingZones geofencingZones,
  ValidationResult validationResult
) {}
