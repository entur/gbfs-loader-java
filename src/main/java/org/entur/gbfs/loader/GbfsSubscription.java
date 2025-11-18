package org.entur.gbfs.loader;

import java.util.concurrent.CompletableFuture;

public interface GbfsSubscription {
  void init();

  boolean getSetupComplete();

  void update();

  /**
   * Get the CompletableFuture for the currently executing update, if any.
   * Returns a completed future if no update is in progress.
   *
   * @return CompletableFuture that completes when the current update finishes
   */
  CompletableFuture<Void> getCurrentUpdate();
}
