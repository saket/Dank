package me.saket.dank.ui.subreddit;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionClickEvent;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionThumbnailClickEvent;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel.SubmissionRowUiModel;
import me.saket.dank.ui.subreddit.uimodels.SubredditSubmission;
import me.saket.dank.ui.subreddit.uimodels.SubredditSubmissionPagination;
import me.saket.dank.utils.InfinitelyScrollableRecyclerViewAdapter;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.walkthrough.SubmissionGestureWalkthroughProceedEvent;
import me.saket.dank.walkthrough.SubmissionGesturesWalkthrough;

public class SubredditSubmissionsAdapter extends RecyclerViewArrayAdapter<SubmissionRowUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<SubmissionRowUiModel>, DiffUtil.DiffResult>>, InfinitelyScrollableRecyclerViewAdapter
{

  public static final int ADAPTER_ID_PAGINATION_FOOTER = -99;
  public static final long ADAPTER_ID_GESTURES_WALKTHROUGH = -98;
  private static final SubmissionRowUiModel.Type[] VIEW_TYPES = SubmissionRowUiModel.Type.values();

  private final Map<SubmissionRowUiModel.Type, SubredditScreenUiModel.SubmissionRowUiChildAdapter> childAdapters;
  private final SubredditSubmission.Adapter submissionAdapter;
  private final SubredditSubmissionPagination.Adapter paginationAdapter;
  private final SubmissionGesturesWalkthrough.Adapter gestureWalkthroughAdapter;

  @Inject
  public SubredditSubmissionsAdapter(
      SubmissionGesturesWalkthrough.Adapter gestureWalkthroughAdapter,
      SubredditSubmission.Adapter submissionAdapter,
      SubredditSubmissionPagination.Adapter paginationAdapter)
  {
    childAdapters = new HashMap<>(4);
    childAdapters.put(SubmissionRowUiModel.Type.GESTURES_WALKTHROUGH, gestureWalkthroughAdapter);
    childAdapters.put(SubmissionRowUiModel.Type.SUBMISSION, submissionAdapter);
    childAdapters.put(SubmissionRowUiModel.Type.PAGINATION_FOOTER, paginationAdapter);

    this.gestureWalkthroughAdapter = gestureWalkthroughAdapter;
    this.paginationAdapter = paginationAdapter;
    this.submissionAdapter = submissionAdapter;
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<SubredditSubmissionClickEvent> submissionClicks() {
    return submissionAdapter.submissionClicks();
  }

  @CheckResult
  public Observable<SubmissionGestureWalkthroughProceedEvent> gestureWalkthroughProceedClicks() {
    return gestureWalkthroughAdapter.proceedClicks();
  }

  @CheckResult
  public Observable<SubredditSubmissionThumbnailClickEvent> thumbnailClicks() {
    return submissionAdapter.thumbnailClicks();
  }

  @CheckResult
  public Observable<SwipeEvent> swipeEvents() {
    return submissionAdapter.swipeEvents();
  }

  @CheckResult
  public Observable<?> paginationFailureRetryClicks() {
    return paginationAdapter.failureRetryClicks();
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  public int getItemCountMinusDecorators() {
    int itemCount = getItemCount();
    if (itemCount > 1 && getItem(itemCount - 1).type() == SubmissionRowUiModel.Type.PAGINATION_FOOTER) {
      itemCount = itemCount - 1;
    }
    return itemCount;
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return childAdapters.get(VIEW_TYPES[viewType]).onCreateViewHolder(inflater, parent);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads);
    } else {
      //noinspection unchecked
      childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position), payloads);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position));
  }

  @Override
  public void accept(Pair<List<SubmissionRowUiModel>, DiffUtil.DiffResult> pair) {
    findCollidingIds(pair.first());

    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }

  /**
   * https://app.bugsnag.com/uncommon-dot-is/dank/errors/5af7391616bc1600193754f8
   */
  @SuppressLint("UseSparseArrays")
  private static void findCollidingIds(List<SubmissionRowUiModel> rows) {
    Map<Long, SubmissionRowUiModel> ids = new HashMap<>(rows.size(), 1f);
    for (SubmissionRowUiModel row : rows) {
      if (ids.containsKey(row.adapterId())) {
        SubmissionRowUiModel existing = ids.get(row.adapterId());
        throw new AssertionError("ID collision between " + existing + " and " + row);
      }
      ids.put(row.adapterId(), row);
    }
  }
}
