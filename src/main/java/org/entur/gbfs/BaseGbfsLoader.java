package org.entur.gbfs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.entur.gbfs.http.GBFSFeedUpdater;

public abstract class BaseGbfsLoader<S> {

  private final AtomicBoolean setupComplete = new AtomicBoolean(false);

  private final Lock updateLock = new ReentrantLock();

  /** One updater per feed type(?)*/
  protected final Map<S, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();

  public AtomicBoolean getSetupComplete() {
    return setupComplete;
  }

  /**
   * Checks if any of the feeds should be updated base on the TTL and fetches. Returns true, if any feeds were updated.
   */
  public boolean update() {
    if (!setupComplete.get()) {
      init();
    }

    boolean didUpdate = false;
    if (updateLock.tryLock()) {
      try {
        for (GBFSFeedUpdater<?> updater : feedUpdaters.values()) {
          if (updater.shouldUpdate()) {
            updater.fetchData();
            if (updater.getData() != null) {
              didUpdate = true;
            }
          }
        }
      } finally {
        // be sure to release lock, even in case an exception is thrown
        updateLock.unlock();
      }
    }
    return didUpdate;
  }

  protected abstract void init();
}
