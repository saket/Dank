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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.InfiniteScrollFooter;
import me.saket.dank.data.InfiniteScrollHeader;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.InfiniteScrollFooterViewHolder;
import me.saket.dank.utils.InfiniteScrollHeaderViewHolder;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;
import timber.log.Timber;

public class SubmissionsAdapter extends RecyclerViewArrayAdapter<Object, RecyclerView.ViewHolder> implements InfinitelyScrollableRecyclerViewAdapter {

  private static final int VIEW_TYPE_HEADER_REFRESHING_ITEMS = 0;
  private static final int VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS = 1;
  private static final int VIEW_TYPE_SUBMISSION = 2;

  private VotingManager votingManager;
  private UserPrefsManager userPrefsManager;
  private OnItemClickListener clickListener;
  private SubmissionSwipeActionsProvider swipeActionsProvider;

  interface OnItemClickListener {
    /**
     * Called when a submission is clicked.
     */
    void onItemClick(Submission submission, View submissionItemView, long submissionId);
  }

  public SubmissionsAdapter(VotingManager votingManager, UserPrefsManager userPrefsManager, SubmissionSwipeActionsProvider swipeActionsProvider) {
    this.votingManager = votingManager;
    this.userPrefsManager = userPrefsManager;
    this.swipeActionsProvider = swipeActionsProvider;
    setHasStableIds(true);
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    clickListener = listener;
  }

  @Override
  public int getItemCountMinusDecorators() {
    List<Object> items = getData();
    int itemCountMinusDecorators = items.size();

    if (itemCountMinusDecorators > 0) {
      if (items.get(0) instanceof InfiniteScrollHeader) {
        itemCountMinusDecorators -= 1;
      }
      if (items.get(items.size() - 1) instanceof InfiniteScrollFooter) {
        itemCountMinusDecorators -= 1;
      }
    }

    return itemCountMinusDecorators;
  }

  @Override
  public int getItemViewType(int position) {
    Object item = getItem(position);
    if (item instanceof InfiniteScrollHeader) {
      return VIEW_TYPE_HEADER_REFRESHING_ITEMS;

    } else if (item instanceof InfiniteScrollFooter) {
      return VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS;

    } else {
      return VIEW_TYPE_SUBMISSION;
    }
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    switch (viewType) {
      case VIEW_TYPE_HEADER_REFRESHING_ITEMS:
        return InfiniteScrollHeaderViewHolder.create(LayoutInflater.from(parent.getContext()), parent);

      case VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS:
        return InfiniteScrollFooterViewHolder.create(LayoutInflater.from(parent.getContext()), parent);

      case VIEW_TYPE_SUBMISSION:
        SubmissionViewHolder holder = SubmissionViewHolder.create(parent);
        holder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider);
        return holder;

      default:
        throw new AssertionError();
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    int itemViewType = getItemViewType(position);

    if (itemViewType == VIEW_TYPE_SUBMISSION) {
      Submission submission = (Submission) getItem(position);
      VoteDirection pendingOrDefaultVoteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
      int submissionScore = votingManager.getScoreAfterAdjustingPendingVote(submission);

      SubmissionViewHolder submissionViewHolder = (SubmissionViewHolder) holder;
      submissionViewHolder.bind(submission, submissionScore, pendingOrDefaultVoteDirection, userPrefsManager.canShowSubmissionCommentsCountInByline());

      // TODO: Avoid creating a new listener for every item.
      holder.itemView.setOnClickListener(v -> {
        clickListener.onItemClick(submission, holder.itemView, getItemId(position));
      });

      // TODO: Avoid creating a new listener for every item.
      SwipeableLayout swipeableLayout = submissionViewHolder.getSwipeableLayout();
      swipeableLayout.setSwipeActions(swipeActionsProvider.getSwipeActions(submission));
      swipeableLayout.setOnPerformSwipeActionListener(action -> {
        swipeActionsProvider.performSwipeAction(action, submission, swipeableLayout);

        // We should ideally only be updating the backing data-set and let onBind() handle the
        // changes, but RecyclerView's item animator reset's the View's x-translation which we
        // don't want. So we manually update the Views here.
        onBindViewHolder(holder, holder.getAdapterPosition());
      });

    } else if (itemViewType == VIEW_TYPE_HEADER_REFRESHING_ITEMS) {
      InfiniteScrollHeader header = (InfiniteScrollHeader) getItem(position);
      Timber.i("header: %s", header);
      ((InfiniteScrollHeaderViewHolder) holder).bind(header);

    } else if (itemViewType == VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS) {
      InfiniteScrollFooter footer = (InfiniteScrollFooter) getItem(position);
      ((InfiniteScrollFooterViewHolder) holder).bind(footer);
    }
  }

  @Override
  public long getItemId(int position) {
    switch (getItemViewType(position)) {
      case VIEW_TYPE_HEADER_REFRESHING_ITEMS:
        return VIEW_TYPE_HEADER_REFRESHING_ITEMS;

      case VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS:
        return VIEW_TYPE_FOOTER_LOADING_MORE_ITEMS;

      case VIEW_TYPE_SUBMISSION:
        Submission submission = (Submission) getItem(position);
        return submission.hashCode();

      default:
        throw new AssertionError();
    }
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
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.subreddit_submission_item_cd_self_text));
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
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.subreddit_submission_item_cd_external_url));
          break;

        case URL:
          // TODO: 05/04/17 Reddit sometimes sends literal "image" as the thumbnail. Use thumbnail variations in that case.
          loadThumbnailFromUrl(submission.getThumbnail());
          thumbnailView.setVisibility(View.VISIBLE);
          thumbnailView.setContentDescription(itemView.getResources().getString(R.string.subreddit_submission_item_cd_external_url));
          break;

        case NONE:
          thumbnailView.setVisibility(View.GONE);
          break;
      }

      Truss titleBuilder = new Truss();
      titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), Commons.voteColor(voteDirection))));
      titleBuilder.append(Strings.abbreviateScore(submissionScore));
      titleBuilder.popSpan();
      titleBuilder.append("  ");
      //noinspection deprecation
      titleBuilder.append(Html.fromHtml(submission.getTitle()));
      titleView.setText(titleBuilder.build());

      if (showCommentsCount) {
        subredditView.setText(itemView.getResources().getString(
            R.string.subreddit_submission_item_byline_subreddit_name_author_and_comments_count,
            submission.getSubredditName(),
            submission.getAuthor(),
            Strings.abbreviateScore(submission.getCommentCount())
        ));
      } else {
        subredditView.setText(itemView.getResources().getString(
            R.string.subreddit_submission_item_byline_subreddit_name_author,
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
      Glide.with(itemView)
          .load(thumbnailUrl)
          .apply(RequestOptions.bitmapTransform(GlideCircularTransformation.INSTANCE))
          .transition(DrawableTransitionOptions.withCrossFade())
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
