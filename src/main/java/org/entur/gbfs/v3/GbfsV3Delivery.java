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

package org.entur.gbfs.v3;

import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;
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
import org.entur.gbfs.validation.model.ValidationResult;

/**
 * This class holds the data for all the GBFS files
 */
public class GbfsV3Delivery {

  private GBFSGbfs discovery;
  private GBFSGbfsVersions version;
  private GBFSSystemInformation systemInformation;
  private GBFSVehicleTypes vehicleTypes;
  private GBFSStationInformation stationInformation;
  private GBFSStationStatus stationStatus;
  private GBFSVehicleStatus vehicleStatus;
  private GBFSSystemRegions systemRegions;
  private GBFSSystemPricingPlans systemPricingPlans;
  private GBFSSystemAlerts systemAlerts;
  private GBFSGeofencingZones geofencingZones;
  private ValidationResult validationResult;

  public GBFSGbfs getDiscovery() {
    return discovery;
  }

  public void setDiscovery(GBFSGbfs discovery) {
    this.discovery = discovery;
  }

  public GBFSGbfsVersions getVersion() {
    return version;
  }

  public void setVersion(GBFSGbfsVersions version) {
    this.version = version;
  }

  public GBFSSystemInformation getSystemInformation() {
    return systemInformation;
  }

  public void setSystemInformation(GBFSSystemInformation systemInformation) {
    this.systemInformation = systemInformation;
  }

  public GBFSVehicleTypes getVehicleTypes() {
    return vehicleTypes;
  }

  public void setVehicleTypes(GBFSVehicleTypes vehicleTypes) {
    this.vehicleTypes = vehicleTypes;
  }

  public GBFSStationInformation getStationInformation() {
    return stationInformation;
  }

  public void setStationInformation(GBFSStationInformation stationInformation) {
    this.stationInformation = stationInformation;
  }

  public GBFSStationStatus getStationStatus() {
    return stationStatus;
  }

  public void setStationStatus(GBFSStationStatus stationStatus) {
    this.stationStatus = stationStatus;
  }

  public GBFSVehicleStatus getVehicleStatus() {
    return vehicleStatus;
  }

  public void setVehicleStatus(GBFSVehicleStatus vehicleStatus) {
    this.vehicleStatus = vehicleStatus;
  }

  public GBFSSystemRegions getSystemRegions() {
    return systemRegions;
  }

  public void setSystemRegions(GBFSSystemRegions systemRegions) {
    this.systemRegions = systemRegions;
  }

  public GBFSSystemPricingPlans getSystemPricingPlans() {
    return systemPricingPlans;
  }

  public void setSystemPricingPlans(GBFSSystemPricingPlans systemPricingPlans) {
    this.systemPricingPlans = systemPricingPlans;
  }

  public GBFSSystemAlerts getSystemAlerts() {
    return systemAlerts;
  }

  public void setSystemAlerts(GBFSSystemAlerts systemAlerts) {
    this.systemAlerts = systemAlerts;
  }

  public GBFSGeofencingZones getGeofencingZones() {
    return geofencingZones;
  }

  public void setGeofencingZones(GBFSGeofencingZones geofencingZones) {
    this.geofencingZones = geofencingZones;
  }

  public ValidationResult getValidationResult() {
    return validationResult;
  }

  public void setValidationResult(ValidationResult validationResult) {
    this.validationResult = validationResult;
  }
}
