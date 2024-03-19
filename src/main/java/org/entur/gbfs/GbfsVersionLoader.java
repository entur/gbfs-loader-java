package org.entur.gbfs;

import java.util.concurrent.atomic.AtomicBoolean;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.gbfs.GBFSFeedName;
import org.entur.gbfs.v3_0_RC2.gbfs.GBFSGbfs;

public interface GbfsVersionLoader {
  AtomicBoolean getSetupComplete();
  boolean update();
  GBFS getDiscoveryFeed();
  GBFSGbfs getV3DiscoveryFeed();
  <T> T getFeed(Class<T> feed);
  byte[] getRawFeed(GBFSFeedName feedName);
  byte[] getRawV3Feed(org.entur.gbfs.v3_0_RC2.gbfs.GBFSFeedName feedName);
}
