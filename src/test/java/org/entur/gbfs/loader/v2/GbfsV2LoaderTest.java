package org.entur.gbfs.loader.v2;

import static org.junit.jupiter.api.Assertions.*;

import com.csvreader.CsvReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.entur.gbfs.http.GBFSHttpClient;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v2_3.station_information.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStationStatus;
import org.mobilitydata.gbfs.v2_3.system_alerts.GBFSSystemAlerts;
import org.mobilitydata.gbfs.v2_3.system_calendar.GBFSSystemCalendar;
import org.mobilitydata.gbfs.v2_3.system_hours.GBFSSystemHours;
import org.mobilitydata.gbfs.v2_3.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v2_3.system_pricing_plans.GBFSSystemPricingPlans;
import org.mobilitydata.gbfs.v2_3.system_regions.GBFSSystemRegions;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests that {@link GbfsV2Loader} handles loading of different versions of GBFS correctly, that the optional
 * language paraameter works correctly, and that the different files in a GBFS bundle are all included, with all
 * information in them.
 */
class GbfsV2LoaderTest {

  public static final String LANGUAGE_NB = "nb";
  public static final String LANGUAGE_EN = "en";
  private static final Logger LOG = LoggerFactory.getLogger(GbfsV2LoaderTest.class);

  @Test
  void getV22FeedWithExplicitLanguage() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
      LANGUAGE_NB
    );

    validateV22Feed(loader);
  }

  @Test
  @Disabled("We need to find a better way to test this")
  void testBackoffStrategy() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/feedwith404feeds/gbfs.json",
      LANGUAGE_NB
    );
    assertTrue(
      loader.update(),
      "First update should return true (even though not successful), as it was at " +
      "least initiated"
    );
    assertFalse(
      loader.update(),
      "Should not update immediately after failing one, even though ttl is 0"
    );
  }

  @Test
  void testSetupCompleteWithNonExistingDiscoveryFile() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/foo/gbfs.json",
      LANGUAGE_NB
    );
    assertFalse(loader.getSetupComplete());
  }

  @Test
  void getV22FeedWithNoLanguage() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json"
    );

    validateV22Feed(loader);
  }

  @Test
  void getV22FeedWithWrongLanguage() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
      LANGUAGE_EN
    );
    assertFalse(loader.getSetupComplete());
  }

  @Test
  void getV10FeedWithExplicitLanguage() {
    GbfsV2Loader loader = new GbfsV2Loader(
      "file:src/test/resources/gbfs/helsinki/gbfs.json",
      LANGUAGE_EN
    );

    validateV10Feed(loader);
  }

  @Test
  @Disabled("Run when needed")
  void fetchAllPublicFeeds() throws IOException {
    InputStream is = new GBFSHttpClient()
      .getData("https://raw.githubusercontent.com/NABSA/gbfs/master/systems.csv");
    CsvReader reader = new CsvReader(is, StandardCharsets.UTF_8);
    reader.readHeaders();
    List<Exception> exceptions = new ArrayList<>();

    while (reader.readRecord()) {
      try {
        String url = reader.get("Auto-Discovery URL");
        new GbfsV2Loader(url).update();
      } catch (Exception e) {
        exceptions.add(e);
      }
    }
    assertTrue(
      exceptions.isEmpty(),
      exceptions.stream().map(Exception::getMessage).collect(Collectors.joining("\n"))
    );
  }

  @Test
  @Disabled("Run when needed")
  void testSpin() {
    assertDoesNotThrow(() -> {
      new GbfsV2Loader("https://gbfs.spin.pm/api/gbfs/v2_2/edmonton/gbfs").update();
    });
  }

  private void validateV22Feed(GbfsV2Loader loader) {
    assertTrue(loader.update());

    GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
    FileValidationResult validationResult = validator.validateFile(
      "system_information",
      new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.SystemInformation).get())
    );
    assertEquals(0, validationResult.errorsCount());

    assertFalse(loader.getRawFeed(GBFSFeedName.GBFS).isEmpty());

    GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
    assertNotNull(systemInformation);
    assertEquals("lillestrombysykkel", systemInformation.getData().getSystemId());
    assertEquals(LANGUAGE_NB, systemInformation.getData().getLanguage());
    assertEquals("Lillestrøm bysykkel", systemInformation.getData().getName());
    assertEquals("Europe/Oslo", systemInformation.getData().getTimezone().value());
    assertNull(systemInformation.getData().getEmail());
    assertNull(systemInformation.getData().getOperator());
    assertNull(systemInformation.getData().getPhoneNumber());
    assertNull(systemInformation.getData().getShortName());
    assertNull(systemInformation.getData().getUrl());

    validationResult =
      validator.validateFile(
        "vehicle_types",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.VehicleTypes).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSVehicleTypes vehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
    assertNotNull(vehicleTypes);
    assertEquals(1, vehicleTypes.getData().getVehicleTypes().size());
    GBFSVehicleType vehicleType = vehicleTypes.getData().getVehicleTypes().get(0);
    assertEquals("YLS:VehicleType:CityBike", vehicleType.getVehicleTypeId());
    assertEquals(GBFSVehicleType.FormFactor.BICYCLE, vehicleType.getFormFactor());
    assertEquals(GBFSVehicleType.PropulsionType.HUMAN, vehicleType.getPropulsionType());
    assertNull(vehicleType.getMaxRangeMeters());

    validationResult =
      validator.validateFile(
        "station_information",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.StationInformation).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSStationInformation stationInformation = loader.getFeed(
      GBFSStationInformation.class
    );
    assertNotNull(stationInformation);
    List<GBFSStation> stations = stationInformation.getData().getStations();
    assertEquals(6, stations.size());
    assertTrue(
      stations.stream().anyMatch(gbfsStation -> gbfsStation.getName().equals("TORVGATA"))
    );
    assertEquals(21, stations.stream().mapToDouble(GBFSStation::getCapacity).sum());

    validationResult =
      validator.validateFile(
        "station_status",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.StationStatus).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
    assertNotNull(stationStatus);
    List<org.mobilitydata.gbfs.v2_3.station_status.GBFSStation> stationStatuses =
      stationStatus.getData().getStations();
    assertEquals(6, stationStatuses.size());

    assertNull(loader.getFeed(GBFSFreeBikeStatus.class));
    assertNull(loader.getFeed(GBFSSystemHours.class));
    assertNull(loader.getFeed(GBFSSystemAlerts.class));
    assertNull(loader.getFeed(GBFSSystemCalendar.class));
    assertNull(loader.getFeed(GBFSSystemRegions.class));

    validationResult =
      validator.validateFile(
        "system_pricing_plans",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.SystemPricingPlans).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSSystemPricingPlans pricingPlans = loader.getFeed(GBFSSystemPricingPlans.class);

    assertNotNull(pricingPlans);
    assertEquals(2, pricingPlans.getData().getPlans().size());

    assertNull(loader.getFeed(GBFSGeofencingZones.class));
  }

  private void validateV10Feed(GbfsV2Loader loader) {
    assertTrue(loader.update());

    GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
    FileValidationResult validationResult = validator.validateFile(
      "system_information",
      new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.SystemInformation).get())
    );
    assertEquals(0, validationResult.errorsCount());

    GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
    assertNotNull(systemInformation);
    assertEquals("HSL_FI_Helsinki", systemInformation.getData().getSystemId());
    assertEquals(LANGUAGE_EN, systemInformation.getData().getLanguage());
    assertEquals("HSL Bikes Share", systemInformation.getData().getName());
    assertEquals("Europe/Helsinki", systemInformation.getData().getTimezone().value());
    assertNull(systemInformation.getData().getEmail());
    assertNull(systemInformation.getData().getOperator());
    assertNull(systemInformation.getData().getPhoneNumber());
    assertNull(systemInformation.getData().getShortName());
    assertNull(systemInformation.getData().getUrl());

    assertNull(loader.getFeed(GBFSVehicleTypes.class));

    validationResult =
      validator.validateFile(
        "station_information",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.StationInformation).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSStationInformation stationInformation = loader.getFeed(
      GBFSStationInformation.class
    );
    assertNotNull(stationInformation);
    List<GBFSStation> stations = stationInformation.getData().getStations();
    assertEquals(10, stations.size());
    assertTrue(
      stations
        .stream()
        .anyMatch(gbfsStation -> gbfsStation.getName().equals("Kaivopuisto"))
    );
    assertEquals(239, stations.stream().mapToDouble(GBFSStation::getCapacity).sum());

    validationResult =
      validator.validateFile(
        "station_status",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeedName.StationStatus).get())
      );
    assertEquals(0, validationResult.errorsCount());

    GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
    assertNotNull(stationStatus);
    List<org.mobilitydata.gbfs.v2_3.station_status.GBFSStation> stationStatuses =
      stationStatus.getData().getStations();
    assertEquals(10, stationStatuses.size());
    assertEquals(
      1,
      stationStatuses.stream().filter(s -> s.getNumBikesAvailable() == 0).count()
    );
    assertEquals(
      10,
      stationStatuses.stream().filter(s -> s.getNumBikesDisabled() == 0).count()
    );
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
