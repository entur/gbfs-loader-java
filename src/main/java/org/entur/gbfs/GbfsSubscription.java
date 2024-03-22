package org.entur.gbfs;

public interface GbfsSubscription {

  void init();

  boolean getSetupComplete();

  void update();
}
