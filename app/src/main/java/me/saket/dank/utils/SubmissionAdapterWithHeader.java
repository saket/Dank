package me.saket.dank.utils;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.subreddits.SubmissionSwipeActionsProvider;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

/**
 * Shows submission details as the header of the comment list.
 */
public class SubmissionAdapterWithHeader extends RecyclerAdapterWithHeader<SubmissionAdapterWithHeader.SubmissionHeaderViewHolder, RecyclerView.ViewHolder> {

  private Submission submission;
  private VotingManager votingManager;
  private SubmissionSwipeActionsProvider swipeActionsProvider;

  public static SubmissionAdapterWithHeader wrap(RecyclerViewArrayAdapter<?, RecyclerView.ViewHolder> commentsAdapter, View headerView,
      VotingManager votingManager, SubmissionSwipeActionsProvider swipeActionsProvider)
  {
    if (headerView.getParent() != null) {
      ((ViewGroup) headerView.getParent()).removeView(headerView);
    }
    return new SubmissionAdapterWithHeader(commentsAdapter, headerView, votingManager, swipeActionsProvider);
  }

  private SubmissionAdapterWithHeader(RecyclerViewArrayAdapter<?, RecyclerView.ViewHolder> adapterToWrap, View headerView,
      VotingManager votingManager, SubmissionSwipeActionsProvider swipeActionsProvider)
  {
    super(adapterToWrap, headerView);
    this.votingManager = votingManager;
    this.swipeActionsProvider = swipeActionsProvider;
  }

  public void updateSubmission(Submission submission) {
    this.submission = submission;
    notifyItemChanged(0);
  }

  @Override
  protected boolean isHeaderVisible() {
    return submission != null;
  }

  @Override
  protected SubmissionHeaderViewHolder onCreateHeaderViewHolder(View headerView) {
    SubmissionHeaderViewHolder holder = new SubmissionHeaderViewHolder(headerView);
    holder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider);
    return holder;
  }

  @Override
  protected void onBindHeaderViewHolder(SubmissionHeaderViewHolder holder, int position) {
    holder.bind(votingManager, getHeaderItem());

    SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
    swipeableLayout.setSwipeActions(swipeActionsProvider.getSwipeActions(submission));
    swipeableLayout.setOnPerformSwipeActionListener(action -> {
      swipeActionsProvider.performSwipeAction(action, submission, swipeableLayout);
      onBindViewHolder(holder, position);
    });
  }

  @Override
  protected Submission getHeaderItem() {
    return submission;
  }

  public static class SubmissionHeaderViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_byline) TextView bylineView;

    public SubmissionHeaderViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(VotingManager votingManager, Submission submission) {
      VoteDirection pendingOrDefaultVote = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
      int voteDirectionColor = Commons.voteColor(pendingOrDefaultVote);

      Truss titleBuilder = new Truss();
      titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), voteDirectionColor)));
      titleBuilder.append(Strings.abbreviateScore(votingManager.getScoreAfterAdjustingPendingVote(submission)));
      titleBuilder.popSpan();
      titleBuilder.append("  ");
      //noinspection deprecation
      titleBuilder.append(Html.fromHtml(submission.getTitle()));
      titleView.setText(titleBuilder.build());

      long timeMillis = JrawUtils.createdTimeUtc(submission);
      bylineView.setText(itemView.getResources().getString(
          R.string.submission_byline,
          submission.getSubredditName(),
          submission.getAuthor(),
          Dates.createTimestamp(itemView.getResources(), timeMillis),
          Strings.abbreviateScore(submission.getCommentCount())
      ));
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
