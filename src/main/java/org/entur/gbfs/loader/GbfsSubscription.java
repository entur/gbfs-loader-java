package org.entur.gbfs.loader;

import java.util.concurrent.CompletableFuture;

public interface GbfsSubscription {
  void init();

  boolean getSetupComplete();

  void update();

  /**
   * Prepares the subscription for an update by setting the CompletableFuture that will track it.
   * This must be called by the manager before scheduling the update to avoid race conditions
   * where unsubscribe is called before the async update task starts.
   *
   * @param future The future that will complete when the update finishes
   */
  void setCurrentUpdate(CompletableFuture<Void> future);

  /**
   * Get the CompletableFuture for the currently executing update, if any.
   * Returns a completed future if no update is in progress.
   *
   * @return CompletableFuture that completes when the current update finishes
   */
  CompletableFuture<Void> getCurrentUpdate();
}
