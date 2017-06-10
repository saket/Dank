package me.saket.dank.ui.subreddits;

import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.utils.GlideCircularTransformation;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public class SubmissionsAdapter extends RecyclerViewArrayAdapter<Submission, SubmissionsAdapter.SubmissionViewHolder>
    implements Consumer<List<Submission>>
{

  private VotingManager votingManager;
  private UserPrefsManager userPrefsManager;
  private OnItemClickListener clickListener;

  interface OnItemClickListener {
    /**
     * Called when a submission is clicked.
     */
    void onItemClick(Submission submission, View submissionItemView, long submissionId);
  }

  public SubmissionsAdapter(VotingManager votingManager, UserPrefsManager userPrefsManager) {
    this.votingManager = votingManager;
    this.userPrefsManager = userPrefsManager;
    setHasStableIds(true);
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    clickListener = listener;
  }

  @Override
  public void accept(List<Submission> submissions) {
    // TODO: 29/01/17 Use DiffUtils / SortedList instead of invalidating the entire list.
    updateData(submissions);
  }

  @Override
  protected SubmissionViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return SubmissionViewHolder.create(parent);
  }

  @Override
  public void onBindViewHolder(SubmissionViewHolder holder, int position) {
    Submission submission = getItem(position);
    VoteDirection pendingOrDefaultVoteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());

    int submissionScore = submission.getScore();

    // Manipulate the score manually if there's a pending vote.
    if (votingManager.isVotePending(submission)) {
      switch (pendingOrDefaultVoteDirection) {
        case UPVOTE:
          submissionScore += 1;
          break;

        case DOWNVOTE:
          submissionScore -= 1;
          break;

        default:
        case NO_VOTE:
          switch (submission.getVote()) {
            case UPVOTE:
              submissionScore -= 1;
              break;

            case DOWNVOTE:
              submissionScore += 1;
              break;

            default:
            case NO_VOTE:
              throw new IllegalStateException("Cannot have a pending 'no-vote' if the last vote was also 'no-vote'");
          }
          break;
      }
    }

    holder.bind(submission, submissionScore, pendingOrDefaultVoteDirection, userPrefsManager.canShowSubmissionCommentsCountInByline());

    holder.itemView.setOnClickListener(v -> {
      clickListener.onItemClick(submission, holder.itemView, getItemId(position));
    });
  }

  @Override
  public long getItemId(int position) {
    Submission submission = getItem(position);

    if (submission == null) {
      // Happens when an item is clicked right when the data-set was getting updated.
      return -1;
    }

    return submission.hashCode();
  }

  public static class SubmissionViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    @BindView(R.id.submission_item_icon) ImageView thumbnailView;
    @BindView(R.id.submission_item_title) TextView titleView;
    @BindView(R.id.submission_item_byline) TextView subredditView;

    @BindColor(R.color.gray_100) int defaultIconsTintColor;

    public SubmissionViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return ((SwipeableLayout) itemView);
    }

    public void bind(Submission submission, int submissionScore, VoteDirection voteDirection, boolean showCommentsCount) {
      //if (submission.getTitle().contains("Already drunk")) {
      //    Timber.d("-------------------------------------------");
      //    Timber.i("%s", submission.getTitle());
      //    Timber.i("Hint: %s, Thumb type: %s", submission.getPostHint(), submission.getThumbnailType());
      //    Timber.i("Thumb: %s", submission.getThumbnail());
      //    Timber.i("Thumbs: %s", submission.getThumbnails());
      //}

      switch (submission.getThumbnailType()) {
        case NSFW:
          // TODO: 08/02/17 NSFW thumbnails
        case SELF:
          loadStaticThumbnail(R.drawable.ic_text_fields_black_24dp);
          thumbnailView.setVisibility(View.VISIBLE);
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.submission_item_cd_self_text));
          break;

        case DEFAULT:
          // Reddit couldn't create a thumbnail. Has to be a URL submission.
          loadStaticThumbnail(R.drawable.ic_link_black_24dp);

          // Get the high-res image for this link.
          // TODO: 19/02/17 Get thumbnails provided by Reddit.
          // TODO: 19/02/17 Don't download high-res images on low-data mode.
          if (submission.getThumbnails() != null) {
            // Resize image.
            loadThumbnailFromUrl(submission.getThumbnails().getSource().getUrl());
          }
          thumbnailView.setVisibility(View.VISIBLE);

          // TODO: Also check this.
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.submission_item_cd_external_url));
          break;

        case URL:
          // TODO: 05/04/17 Reddit sometimes sends literal "image" as the thumbnail. Use thumbnail variations in that case.
          loadThumbnailFromUrl(submission.getThumbnail());
          thumbnailView.setVisibility(View.VISIBLE);
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.submission_item_cd_external_url));
          break;

        case NONE:
          thumbnailView.setVisibility(View.GONE);
          break;
      }

      int voteDirectionColor;
      switch (voteDirection) {
        case UPVOTE:
          voteDirectionColor = R.color.submission_item_vote_direction_upvote;
          break;

        case DOWNVOTE:
          voteDirectionColor = R.color.submission_item_vote_direction_downvote;
          break;

        default:
        case NO_VOTE:
          voteDirectionColor = R.color.submission_item_vote_direction_none;
          break;
      }

      Truss titleBuilder = new Truss();
      titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), voteDirectionColor)));
      titleBuilder.append(Strings.abbreviateScore(submissionScore));
      titleBuilder.popSpan();
      titleBuilder.append("  ");
      //noinspection deprecation
      titleBuilder.append(Html.fromHtml(submission.getTitle()));
      titleView.setText(titleBuilder.build());

      if (showCommentsCount) {
        subredditView.setText(itemView.getResources().getString(
            R.string.submission_item_byline_subreddit_name_author_and_comments_count,
            submission.getSubredditName(),
            submission.getAuthor(),
            Strings.abbreviateScore(submission.getCommentCount())
        ));
      } else {
        subredditView.setText(itemView.getResources().getString(
            R.string.submission_item_byline_subreddit_name_author,
            submission.getSubredditName(),
            submission.getAuthor()
        ));
      }
    }

    private void loadStaticThumbnail(@DrawableRes int iconRes) {
      thumbnailView.setBackgroundResource(R.drawable.background_submission_self_thumbnail);
      thumbnailView.setImageResource(iconRes);
      thumbnailView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      thumbnailView.setColorFilter(defaultIconsTintColor);
    }

    private void loadThumbnailFromUrl(String thumbnailUrl) {
      Glide.with(itemView.getContext()).load(thumbnailUrl)
          .bitmapTransform(new GlideCircularTransformation(itemView.getContext()))
          .into(thumbnailView);

      thumbnailView.setColorFilter(null);
      thumbnailView.setBackground(null);
      thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public static SubmissionViewHolder create(ViewGroup parent) {
      return new SubmissionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_submission, parent, false));
    }
  }
}
