package me.saket.dank.ui.subreddits;

import java.util.List;

import me.saket.dank.data.SubredditSubscription;

public class SubredditSubscriptionDiffCallbacks extends SimpleDiffUtilsCallbacks<SubredditSubscription> {

  public SubredditSubscriptionDiffCallbacks(List<SubredditSubscription> oldSubscriptions, List<SubredditSubscription> newSubscriptions) {
    super(oldSubscriptions, newSubscriptions);
  }

  @Override
  protected boolean areContentsTheSame(SubredditSubscription oldSubscription, SubredditSubscription newSubscription) {
    return oldSubscription.name().equals(newSubscription.name());
  }

  @Override
  public boolean areItemsTheSame(SubredditSubscription oldSubscription, SubredditSubscription newSubscription) {
    return oldSubscription.equals(newSubscription);
  }
}
