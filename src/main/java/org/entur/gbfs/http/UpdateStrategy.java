package org.entur.gbfs.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to schedule nextUpdate, depending on lastUpdated, ttl and number of recently failed
 * request attempts. In case of a failing request, e.g. because the remote site is not available
 * or a quota is exceeded, this strategy backs off exponentially, up to a max backoff of 1 hour.
 * To avoid that aa large number of requests is scheduled at exactly the same time, we subtract
 * a random amount up to 5% of the backoff time.
 */
public class UpdateStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateStrategy.class);

  private static int MAX_BACKOFF_SECONDS = 3600;
  private int failedAttemptsCount = 0;
  private int nextUpdate;

  public boolean shouldUpdate() {
    return getCurrentTimeSeconds() >= nextUpdate;
  }

  public void rescheduleAfterFailure() {
    failedAttemptsCount++;
    int backoffSeconds = Math.min(
      MAX_BACKOFF_SECONDS,
      (int) Math.pow(2, failedAttemptsCount - 1)
    );
    // subtract a random value up to 5% to spread requests
    int randomOffset = (int) (Math.random() * 0.05 * backoffSeconds);
    nextUpdate = getCurrentTimeSeconds() + backoffSeconds - randomOffset;
    LOG.info(
      "Rescheduled nextUpdate after {} failure(s) to {}",
      failedAttemptsCount,
      nextUpdate
    );
  }

  private int getCurrentTimeSeconds() {
    return (int) (System.currentTimeMillis() / 1000);
  }

  public void scheduleNextUpdate(Integer lastUpdated, Integer ttl) {
    failedAttemptsCount = 0;
    if (lastUpdated == null || ttl == null) {
      nextUpdate = getCurrentTimeSeconds();
    } else {
      nextUpdate = lastUpdated + ttl;
    }
  }
}
