package org.entur.gbfs.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
