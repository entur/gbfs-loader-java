package org.entur.gbfs.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class UpdateStrategyTest {

  @Test
  void testUpdateImmediately() {
    UpdateStrategy subject = new UpdateStrategy();
    subject.scheduleNextUpdate((int) Instant.now().getEpochSecond(), 0);
    assertTrue(subject.shouldUpdate());
  }

  @Test
  void testUpdateLater() {
    UpdateStrategy subject = new UpdateStrategy();
    subject.scheduleNextUpdate((int) Instant.now().getEpochSecond(), 10);
    assertFalse(subject.shouldUpdate());
  }

  @Test
  @Disabled(
    "This test is flaky due to rescheduleAfterFailure does not always leave enough time for the next assertion"
  )
  void testUpdateFailed() {
    UpdateStrategy subject = new UpdateStrategy();
    subject.scheduleNextUpdate((int) Instant.now().getEpochSecond(), 0);
    assertTrue(subject.shouldUpdate());
    subject.rescheduleAfterFailure();
    assertFalse(subject.shouldUpdate());
  }
}
