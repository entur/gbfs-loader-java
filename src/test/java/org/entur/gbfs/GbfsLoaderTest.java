package org.entur.gbfs;

import com.csvreader.CsvReader;
import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_2.station_information.GBFSStation;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_2.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_2.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_2.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_2.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests that {@link GbfsLoader} handles loading of different versions of GBFS correctly, that the optional
 * language paraameter works correctly, and that the different files in a GBFS bundle are all included, with all
 * information in them.
 */
public class GbfsLoaderTest {
    public static final String LANGUAGE_NB = "nb";
    public static final String LANGUAGE_EN = "en";
    private static final Logger LOG = LoggerFactory.getLogger(GbfsLoaderTest.class);

    @Test
    void getV22FeedWithExplicitLanguage() {
        GbfsLoader loader = new GbfsLoader(
                "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
                null,
                LANGUAGE_NB
        );

        validateV22Feed(loader);
    }

    @Test
    void getV22FeedWithNoLanguage() {
        GbfsLoader loader = new GbfsLoader(
                "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
                null,
                null
        );

        validateV22Feed(loader);
    }

    @Test
    void getV22FeedWithWrongLanguage() {
        assertThrows(RuntimeException.class, () -> new GbfsLoader(
                "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
                null,
                LANGUAGE_EN
        ));
    }

    @Test
    void getV10FeedWithExplicitLanguage() {
        GbfsLoader loader = new GbfsLoader(
                "file:src/test/resources/gbfs/helsinki/gbfs.json",
                null,
                LANGUAGE_EN
        );

        validateV10Feed(loader);
    }

    @Test
    @Disabled
    void fetchAllPublicFeeds() throws IOException {
        InputStream is = HttpUtils.getData("https://raw.githubusercontent.com/NABSA/gbfs/master/systems.csv");
        CsvReader reader = new CsvReader(is, StandardCharsets.UTF_8);
        reader.readHeaders();
        List<Exception> exceptions = new ArrayList<>();

        while (reader.readRecord()) {
            try {
                String url = reader.get("Auto-Discovery URL");
                new GbfsLoader(url, null, null).update();
            } catch (Exception e) {
                exceptions.add(e);
            }

        }
        assertTrue(exceptions.isEmpty(), exceptions.stream().map(Exception::getMessage).collect(Collectors.joining("\n")));
    }

    @Test
    @Disabled
    void testSpin() {
        new GbfsLoader("https://gbfs.spin.pm/api/gbfs/v2_2/edmonton/gbfs", null, null).update();
    }

    private void validateV22Feed(GbfsLoader loader) {
        assertTrue(loader.update());

        GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
        assertNotNull(systemInformation);
        assertEquals("lillestrombysykkel", systemInformation.getData().getSystemId());
        assertEquals(LANGUAGE_NB, systemInformation.getData().getLanguage());
        assertEquals("Lillestrøm bysykkel", systemInformation.getData().getName());
        assertEquals("Europe/Oslo", systemInformation.getData().getTimezone());
        assertNull(systemInformation.getData().getEmail());
        assertNull(systemInformation.getData().getOperator());
        assertNull(systemInformation.getData().getPhoneNumber());
        assertNull(systemInformation.getData().getShortName());
        assertNull(systemInformation.getData().getUrl());


        GBFSVehicleTypes vehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
        assertNotNull(vehicleTypes);
        assertEquals(1, vehicleTypes.getData().getVehicleTypes().size());
        GBFSVehicleType vehicleType = vehicleTypes.getData().getVehicleTypes().get(0);
        assertEquals("YLS:VehicleType:CityBike", vehicleType.getVehicleTypeId());
        assertEquals(GBFSVehicleType.FormFactor.BICYCLE, vehicleType.getFormFactor());
        assertEquals(GBFSVehicleType.PropulsionType.HUMAN, vehicleType.getPropulsionType());
        assertNull(vehicleType.getMaxRangeMeters());


        GBFSStationInformation stationInformation = loader.getFeed(GBFSStationInformation.class);
        assertNotNull(stationInformation);
        List<GBFSStation> stations = stationInformation.getData().getStations();
        assertEquals(6, stations.size());
        assertTrue(stations.stream().anyMatch(gbfsStation -> gbfsStation.getName().equals("TORVGATA")));
        assertEquals(21, stations.stream().mapToDouble(GBFSStation::getCapacity).sum());


        GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
        assertNotNull(stationStatus);
        List<org.entur.gbfs.v2_2.station_status.GBFSStation> stationStatuses = stationStatus.getData().getStations();
        assertEquals(6, stationStatuses.size());

        assertNull(loader.getFeed(GBFSFreeBikeStatus.class));
        assertNull(loader.getFeed(GBFSSystemHours.class));
        assertNull(loader.getFeed(GBFSSystemAlerts.class));
        assertNull(loader.getFeed(GBFSSystemCalendar.class));
        assertNull(loader.getFeed(GBFSSystemRegions.class));

        GBFSSystemPricingPlans pricingPlans = loader.getFeed(GBFSSystemPricingPlans.class);

        assertNotNull(pricingPlans);
        assertEquals(2, pricingPlans.getData().getPlans().size());

        assertNull(loader.getFeed(GBFSGeofencingZones.class));
    }


    private void validateV10Feed(GbfsLoader loader) {
        assertTrue(loader.update());

        GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
        assertNotNull(systemInformation);
        assertEquals("HSL_FI_Helsinki", systemInformation.getData().getSystemId());
        assertEquals(LANGUAGE_EN, systemInformation.getData().getLanguage());
        assertEquals("HSL Bikes Share", systemInformation.getData().getName());
        assertEquals("Europe/Helsinki", systemInformation.getData().getTimezone());
        assertNull(systemInformation.getData().getEmail());
        assertNull(systemInformation.getData().getOperator());
        assertNull(systemInformation.getData().getPhoneNumber());
        assertNull(systemInformation.getData().getShortName());
        assertNull(systemInformation.getData().getUrl());


        assertNull(loader.getFeed(GBFSVehicleTypes.class));


        GBFSStationInformation stationInformation = loader.getFeed(GBFSStationInformation.class);
        assertNotNull(stationInformation);
        List<GBFSStation> stations = stationInformation.getData().getStations();
        assertEquals(10, stations.size());
        assertTrue(stations.stream().anyMatch(gbfsStation -> gbfsStation.getName().equals("Kaivopuisto")));
        assertEquals(239, stations.stream().mapToDouble(GBFSStation::getCapacity).sum());


        GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
        assertNotNull(stationStatus);
        List<org.entur.gbfs.v2_2.station_status.GBFSStation> stationStatuses = stationStatus.getData().getStations();
        assertEquals(10, stationStatuses.size());
        assertEquals(1, stationStatuses.stream().filter(s -> s.getNumBikesAvailable() == 0).count());
        assertEquals(10, stationStatuses.stream().filter(s -> s.getNumBikesDisabled() == 0).count());
        assertEquals(1, stationStatuses.stream().filter(s -> !s.getIsRenting()).count());
        assertEquals(1, stationStatuses.stream().filter(s -> !s.getIsReturning()).count());

        assertNull(loader.getFeed(GBFSFreeBikeStatus.class));
        assertNull(loader.getFeed(GBFSSystemHours.class));
        assertNull(loader.getFeed(GBFSSystemAlerts.class));
        assertNull(loader.getFeed(GBFSSystemCalendar.class));
        assertNull(loader.getFeed(GBFSSystemRegions.class));
        assertNull(loader.getFeed(GBFSSystemPricingPlans.class));
        assertNull(loader.getFeed(GBFSGeofencingZones.class));
    }
}
