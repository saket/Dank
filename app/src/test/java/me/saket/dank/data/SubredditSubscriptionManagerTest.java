package me.saket.dank.data;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SubredditSubscriptionManagerTest {

  private SubredditSubscriptionManager subscriptionManager;

  @Before
  public void setUp() throws Exception {
    subscriptionManager = new SubredditSubscriptionManager(null, null, null, null, null);
  }

  @Test
  public void mergeRemoteSubscriptionsWithLocal() throws Exception {
    List<SubredditSubscription> localSubs = new ArrayList<>();
    localSubs.add(SubredditSubscription.create("A", SubredditSubscription.PendingState.NONE, false));
    localSubs.add(SubredditSubscription.create("B", SubredditSubscription.PendingState.NONE, true));
    localSubs.add(SubredditSubscription.create("C", SubredditSubscription.PendingState.NONE, true));
    localSubs.add(SubredditSubscription.create("D", SubredditSubscription.PendingState.PENDING_SUBSCRIBE, false));
    localSubs.add(SubredditSubscription.create("E", SubredditSubscription.PendingState.PENDING_UNSUBSCRIBE, false));
    localSubs.add(SubredditSubscription.create("F", SubredditSubscription.PendingState.PENDING_UNSUBSCRIBE, false));

    List<String> remoteSubNames = new ArrayList<>();
    remoteSubNames.add("A");
    remoteSubNames.add("B");
    remoteSubNames.add("E");
    remoteSubNames.add("G");
    remoteSubNames.add("H");

    List<SubredditSubscription> expectedMergedList = new ArrayList<>();
    expectedMergedList.add(SubredditSubscription.create("A", SubredditSubscription.PendingState.NONE, false));
    expectedMergedList.add(SubredditSubscription.create("B", SubredditSubscription.PendingState.NONE, true));
    expectedMergedList.add(SubredditSubscription.create("D", SubredditSubscription.PendingState.PENDING_SUBSCRIBE, false));
    expectedMergedList.add(SubredditSubscription.create("E", SubredditSubscription.PendingState.PENDING_UNSUBSCRIBE, false));
    expectedMergedList.add(SubredditSubscription.create("G", SubredditSubscription.PendingState.NONE, false));
    expectedMergedList.add(SubredditSubscription.create("H", SubredditSubscription.PendingState.NONE, false));

    List<SubredditSubscription> mergedList = subscriptionManager.mergeRemoteSubscriptionsWithLocal(localSubs).apply(remoteSubNames);
    Assert.assertEquals(expectedMergedList, mergedList);
  }

}
