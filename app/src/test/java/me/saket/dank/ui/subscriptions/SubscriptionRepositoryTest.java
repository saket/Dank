package me.saket.dank.ui.subscriptions;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import me.saket.dank.reddit.Reddit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubscriptionRepositoryTest {

  private SubscriptionRepository subscriptionRepository;

  @Before
  public void setUp() {
    //noinspection ConstantConditions
    subscriptionRepository = new SubscriptionRepository(
        () -> {
          Reddit reddit = mock(Reddit.class, RETURNS_DEEP_STUBS);
          when(reddit.subscriptions().needsRemoteSubscription(any())).thenReturn(true);
          return reddit;
        },
        null,
        null,
        null,
        null);
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

    List<SubredditSubscription> mergedList = subscriptionRepository.mergeRemoteSubscriptionsWithLocal(localSubs).apply(remoteSubNames);
    assertEquals(expectedMergedList, mergedList);
  }
}
