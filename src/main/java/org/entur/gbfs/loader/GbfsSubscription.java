package org.entur.gbfs.loader;

public interface GbfsSubscription {
  void init();

  boolean getSetupComplete();

  void update();
}
