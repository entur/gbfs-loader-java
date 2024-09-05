package org.entur.gbfs.loader;

public interface GbfsSubscription {
  void init();

  boolean getSetupComplete();

  void beforeUpdate();

  void update();

  void afterUpdate();
}
