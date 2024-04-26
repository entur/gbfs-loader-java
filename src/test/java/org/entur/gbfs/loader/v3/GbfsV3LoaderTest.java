package org.entur.gbfs.loader.v3;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.entur.gbfs.v3_0.gbfs.GBFSFeed;
import org.entur.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.entur.gbfs.v3_0.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v3_0.vehicle_status.GBFSVehicle;
import org.entur.gbfs.v3_0.vehicle_status.GBFSVehicleStatus;
import org.entur.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.entur.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests that {@link GbfsV3Loader} handles loading correctly,
 */
class GbfsV3LoaderTest {

  public static final String LANGUAGE_NB = "nb-NO";
  public static final String LANGUAGE_EN = "en";
  private static final Logger LOG = LoggerFactory.getLogger(GbfsV3LoaderTest.class);

  @Test
  void getV3Feed() {
    GbfsV3Loader loader = new GbfsV3Loader(
      "file:src/test/resources/gbfs/v3/getaroundstavanger/gbfs.json"
    );

    validateV3Feed(loader);
  }

  private void validateV3Feed(GbfsV3Loader loader) {
    assertTrue(loader.update());

    GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
    FileValidationResult validationResult = validator.validateFile(
      "system_information",
      new ByteArrayInputStream(loader.getRawFeed(GBFSFeed.Name.SYSTEM_INFORMATION).get())
    );
    assertEquals(0, validationResult.getErrorsCount());

    assertFalse(loader.getRawFeed(GBFSFeed.Name.GBFS).isEmpty());

    GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
    assertNotNull(systemInformation);
    assertEquals("getaround_stavanger", systemInformation.getData().getSystemId());
    assertTrue(systemInformation.getData().getLanguages().contains(LANGUAGE_NB));
    assertEquals("Getaround", systemInformation.getData().getName().get(0).getText());
    assertEquals("Europe/Oslo", systemInformation.getData().getTimezone().value());
    assertNull(systemInformation.getData().getEmail());
    assertNull(systemInformation.getData().getOperator());
    assertNull(systemInformation.getData().getPhoneNumber());
    assertNull(systemInformation.getData().getShortName());
    assertEquals(
      "https://no.getaround.com/bilutleie/stavanger",
      systemInformation.getData().getUrl()
    );

    validationResult =
      validator.validateFile(
        "vehicle_types",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeed.Name.VEHICLE_TYPES).get())
      );
    assertEquals(0, validationResult.getErrorsCount());

    GBFSVehicleTypes vehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
    assertNotNull(vehicleTypes);
    assertEquals(4, vehicleTypes.getData().getVehicleTypes().size());
    GBFSVehicleType vehicleType = vehicleTypes.getData().getVehicleTypes().get(0);
    assertEquals(
      "YGA:VehicleType:car-generic-combustion",
      vehicleType.getVehicleTypeId()
    );
    assertEquals(GBFSVehicleType.FormFactor.CAR, vehicleType.getFormFactor());
    assertEquals(
      GBFSVehicleType.PropulsionType.COMBUSTION,
      vehicleType.getPropulsionType()
    );
    assertEquals(400000, vehicleType.getMaxRangeMeters());

    validationResult =
      validator.validateFile(
        "vehicle_status",
        new ByteArrayInputStream(loader.getRawFeed(GBFSFeed.Name.VEHICLE_STATUS).get())
      );
    assertEquals(0, validationResult.getErrorsCount());

    GBFSVehicleStatus vehicleStatus = loader.getFeed(GBFSVehicleStatus.class);
    assertNotNull(vehicleStatus);
    List<GBFSVehicle> vecicles = vehicleStatus.getData().getVehicles();
    assertEquals(40, vecicles.size());
    assertTrue(
      vecicles
        .stream()
        .anyMatch(vehicle -> vehicle.getVehicleId().equals("YGA:Vehicle:1218349"))
    );

    validationResult =
      validator.validateFile(
        "system_pricing_plans",
        new ByteArrayInputStream(
          loader.getRawFeed(GBFSFeed.Name.SYSTEM_PRICING_PLANS).get()
        )
      );
    assertEquals(0, validationResult.getErrorsCount());

    GBFSSystemPricingPlans pricingPlans = loader.getFeed(GBFSSystemPricingPlans.class);

    assertNotNull(pricingPlans);
    assertEquals(40, pricingPlans.getData().getPlans().size());

    assertNull(loader.getFeed(GBFSGeofencingZones.class));
  }

  @Test
  void testDuplicateFeedThrows() {
    GbfsV3Loader loader = new GbfsV3Loader(
      "file:src/test/resources/gbfs/v3/duplicatefeed/gbfs.json"
    );
    assertFalse(loader.getSetupComplete());
  }

  @Test
  void testIncompleteFeed() {
    GbfsV3Loader loader = new GbfsV3Loader(
      "file:src/test/resources/gbfs/v3/incomplete/gbfs.json"
    );
    assertFalse(loader.getSetupComplete());
  }
}
