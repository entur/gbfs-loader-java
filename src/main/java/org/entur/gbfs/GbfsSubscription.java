package org.entur.gbfs;

public interface GbfsSubscription {
  public void init();

  public boolean getSetupComplete();

  public void update();
}
