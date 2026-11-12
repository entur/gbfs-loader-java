package org.entur.gbfs.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSGbfs;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.mockito.Mock;
import org.mockito.Mockito;

class GBFSFeedUpdaterTest {

  @Mock
  GBFSHttpClient httpClientMock;

  @Mock
  UpdateStrategy updateStrategyMock;

  GBFSFeedUpdater<GBFSGbfs> subject;

  @BeforeEach
  void setup() {
    httpClientMock = Mockito.mock(GBFSHttpClient.class);
    updateStrategyMock = Mockito.mock(UpdateStrategy.class);

    subject =
      new GBFSFeedUpdater<>(
        URI.create("https://test.com/gbfs"),
        new DummyRequestAuthenticator(),
        GBFSGbfs.class,
        null,
        null,
        httpClientMock,
        updateStrategyMock
      );
  }

  @Test
  void testUpdateReturnsFalseWhenUpdateStrategyShouldUpdateReturnsFalse() {
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(false);
    assertFalse(subject.update());
  }

  @Test
  void testUpdateReturnsFalseWhenHttpClientReturnsNoData() throws IOException {
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(null);
    assertFalse(subject.update());
  }

  @Test
  void testUpdateReturnsFalseWhenUnmarshallingDataFails() throws IOException {
    String initialString = "Not JSON";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);
    assertFalse(subject.update());
  }

  @Test
  void testUpdateReturnsFalseWhenInvalidData() throws IOException {
    String initialString = "{}";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);
    assertFalse(subject.update());
  }

  @Test
  void testUpdateReturnsFalseWhenClientThrows() throws IOException {
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito
      .when(httpClientMock.getData(any(), any(), any()))
      .thenThrow(IOException.class);
    assertFalse(subject.update());
  }

  @Test
  void testHappyPath() throws IOException {
    String initialString =
      "{\"last_updated\":\"2024-03-21T09:25:53.343Z\",\"ttl\":0,\"version\":\"3.0-RC2\",\"data\":{\"feeds\":[{\"name\":\"system_information\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/system_information.json\"},{\"name\":\"vehicle_types\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/vehicle_types.json\"},{\"name\":\"vehicle_status\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/vehicle_status.json\"},{\"name\":\"system_pricing_plans\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/system_pricing_plans.json\"}]}}";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);
    assertTrue(subject.update());
  }

  @Test
  void testRfc3339TimestampWithOffsetIsAcceptedForV3() throws IOException {
    String initialString =
      "{\"last_updated\":\"2024-03-21T09:25:53+01:00\",\"ttl\":0,\"version\":\"3.0\",\"data\":{\"feeds\":[{\"name\":\"system_information\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/system_information.json\"}]}}";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);
    assertTrue(subject.update());
  }

  @Test
  void testNumericTimestampIsRejectedForV3() throws IOException {
    String initialString =
      "{\"last_updated\":1711013153,\"ttl\":0,\"version\":\"3.0\",\"data\":{\"feeds\":[{\"name\":\"system_information\",\"url\":\"file:src/test/resources/gbfs/v3/getaroundstavanger/system_information.json\"}]}}";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);
    assertFalse(subject.update());
  }

  @Test
  void testGeoJsonGeometryIsDeserialized() throws IOException {
    String initialString =
      "{\"last_updated\":\"2024-03-21T09:25:53+01:00\",\"ttl\":60,\"version\":\"3.0\",\"data\":{\"geofencing_zones\":{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-122.57,45.56],[-122.66,45.56],[-122.66,45.50],[-122.57,45.56]]]]},\"properties\":{}}]},\"global_rules\":[]}}";
    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Mockito.when(updateStrategyMock.shouldUpdate()).thenReturn(true);
    Mockito.when(httpClientMock.getData(any(), any(), any())).thenReturn(targetStream);

    GBFSFeedUpdater<GBFSGeofencingZones> geofencingSubject = new GBFSFeedUpdater<>(
      URI.create("https://test.com/geofencing_zones"),
      new DummyRequestAuthenticator(),
      GBFSGeofencingZones.class,
      null,
      null,
      httpClientMock,
      updateStrategyMock
    );

    assertTrue(geofencingSubject.update());
    assertEquals(
      -122.57,
      geofencingSubject
        .getData()
        .getData()
        .getGeofencingZones()
        .getFeatures()
        .get(0)
        .getGeometry()
        .getCoordinates()
        .get(0)
        .get(0)
        .get(0)
        .getLongitude()
    );
  }
}
