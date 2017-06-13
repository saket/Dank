package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import net.dean.jraw.models.Submission;

import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;
import timber.log.Timber;

/**
 * Shows submission details as the header of the comment list.
 */
public class SubmissionAdapterWithHeader extends RecyclerAdapterWithHeader<RecyclerView.ViewHolder> {

  private Submission submission;

  public static SubmissionAdapterWithHeader wrap(RecyclerViewArrayAdapter<?, RecyclerView.ViewHolder> commentsAdapter, View headerView) {
    if (headerView.getParent() != null) {
      ((ViewGroup) headerView.getParent()).removeView(headerView);
    }
    return new SubmissionAdapterWithHeader(commentsAdapter, headerView);
  }

  private SubmissionAdapterWithHeader(RecyclerViewArrayAdapter<?, RecyclerView.ViewHolder> adapterToWrap, View headerView) {
    super(adapterToWrap, headerView);
  }

  public void updateData(Submission submission) {
    this.submission = submission;
  }

  @Override
  protected RecyclerView.ViewHolder onCreateHeaderViewHolder(View headerView) {
    return new SubmissionHeaderViewHolder(headerView);
  }

  @Override
  protected void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
    ((SubmissionHeaderViewHolder) holder).bind(getHeaderItem());
  }

  @Override
  protected Submission getHeaderItem() {
    return submission;
  }

  public static class SubmissionHeaderViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    public SubmissionHeaderViewHolder(View itemView) {
      super(itemView);
    }

    public void bind(Submission submission) {
      // TODO.
      if (submission == null) {
        Timber.w("Submission is null");
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
