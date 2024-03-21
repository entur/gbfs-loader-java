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
    subject.scheduleNextUpdate(null, null);
    assertTrue(subject.shouldUpdate());
  }

  @Test
  void testUpdateLater() {
    UpdateStrategy subject = new UpdateStrategy();
    subject.scheduleNextUpdate((int) Instant.now().getEpochSecond(), 10);
    assertFalse(subject.shouldUpdate());
  }

  @Test
  void testUpdateFailed() {
    UpdateStrategy subject = new UpdateStrategy();
    subject.scheduleNextUpdate((int) Instant.now().getEpochSecond(), 0);
    assertTrue(subject.shouldUpdate());

    // run rescheduleAfterFailure mulitple times to increase chance that
    // the following assertion has "time to fail"
    subject.rescheduleAfterFailure();
    subject.rescheduleAfterFailure();
    subject.rescheduleAfterFailure();

    assertFalse(subject.shouldUpdate());
  }
}
