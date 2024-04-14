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

import org.entur.gbfs.v3_0.gbfs.GBFSGbfs;
import org.entur.gbfs.v3_0.gbfs_versions.GBFSGbfsVersions;
import org.entur.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v3_0.station_information.GBFSStationInformation;
import org.entur.gbfs.v3_0.station_status.GBFSStationStatus;
import org.entur.gbfs.v3_0.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.entur.gbfs.v3_0.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v3_0.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v3_0.vehicle_status.GBFSVehicleStatus;
import org.entur.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.entur.gbfs.validation.model.ValidationResult;

/**
 * This class holds the data for all the GBFS files
 */
public record GbfsV3Delivery(
  GBFSGbfs discovery,
  GBFSGbfsVersions version,
  GBFSSystemInformation systemInformation,
  GBFSVehicleTypes vehicleTypes,
  GBFSStationInformation stationInformation,
  GBFSStationStatus stationStatus,
  GBFSVehicleStatus vehicleStatus,
  GBFSSystemRegions systemRegions,
  GBFSSystemPricingPlans systemPricingPlans,
  GBFSSystemAlerts systemAlerts,
  GBFSGeofencingZones geofencingZones,
  ValidationResult validationResult
) {}
