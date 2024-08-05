package org.entur.gbfs;

/**
 * An interceptor that can be notified before and after a subscription's update
 */
public interface SubscriptionUpdateInterceptor {
  /**
   * Called before a subscription is updated
   */
  void beforeUpdate();

  /**
   * Called after a subscription was updated
   */
  void afterUpdate();
}
