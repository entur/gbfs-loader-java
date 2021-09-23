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

/**
 * This class holds the data for all the GBFS files
 */
public class GbfsDelivery {
    private GBFS discovery;
    private GBFSGbfsVersions version;
    private GBFSSystemInformation systemInformation;
    private GBFSVehicleTypes vehicleTypes;
    private GBFSStationInformation stationInformation;
    private GBFSStationStatus stationStatus;
    private GBFSFreeBikeStatus freeBikeStatus;
    private GBFSSystemHours systemHours;
    private GBFSSystemCalendar systemCalendar;
    private GBFSSystemRegions systemRegions;
    private GBFSSystemPricingPlans systemPricingPlans;
    private GBFSSystemAlerts systemAlerts;
    private GBFSGeofencingZones geofencingZones;

    public void setFeed(Object feed) {
        if (feed instanceof GBFS) {
            setDiscovery((GBFS) feed);
        }
    }

    public GBFS getDiscovery() {
        return discovery;
    }

    public void setDiscovery(GBFS discovery) {
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

    public GBFSFreeBikeStatus getFreeBikeStatus() {
        return freeBikeStatus;
    }

    public void setFreeBikeStatus(GBFSFreeBikeStatus freeBikeStatus) {
        this.freeBikeStatus = freeBikeStatus;
    }

    public GBFSSystemHours getSystemHours() {
        return systemHours;
    }

    public void setSystemHours(GBFSSystemHours systemHours) {
        this.systemHours = systemHours;
    }

    public GBFSSystemCalendar getSystemCalendar() {
        return systemCalendar;
    }

    public void setSystemCalendar(GBFSSystemCalendar systemCalendar) {
        this.systemCalendar = systemCalendar;
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
}
