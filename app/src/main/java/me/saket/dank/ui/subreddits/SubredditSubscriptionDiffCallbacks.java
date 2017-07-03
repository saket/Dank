package me.saket.dank.ui.subreddits;

import android.support.v7.util.DiffUtil;

import java.util.List;

import me.saket.dank.data.SubredditSubscription;

public class SubredditSubscriptionDiffCallbacks extends DiffUtil.Callback {

  private final List<SubredditSubscription> oldSubscriptions;
  private final List<SubredditSubscription> newSubscriptions;

  public SubredditSubscriptionDiffCallbacks(List<SubredditSubscription> oldSubscriptions, List<SubredditSubscription> newSubscriptions) {
    this.oldSubscriptions = oldSubscriptions;
    this.newSubscriptions = newSubscriptions;
  }

  @Override
  public int getOldListSize() {
    return oldSubscriptions.size();
  }

  @Override
  public int getNewListSize() {
    return newSubscriptions.size();
  }

  @Override
  public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
    SubredditSubscription oldSubscription = oldSubscriptions.get(oldItemPosition);
    SubredditSubscription newSubscription = newSubscriptions.get(newItemPosition);
    return oldSubscription.name().equals(newSubscription.name());
  }

  @Override
  public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
    SubredditSubscription oldSubscription = oldSubscriptions.get(oldItemPosition);
    SubredditSubscription newSubscription = newSubscriptions.get(newItemPosition);
    return oldSubscription.equals(newSubscription);
  }
}
