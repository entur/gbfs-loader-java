package org.entur.gbfs;

public interface SubscriptionUpdateInterceptor {
  void beforeUpdate();
  void afterUpdate();
}
