package me.saket.dank.ui.subreddits;

import android.support.annotation.CheckResult;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.ui.subreddits.models.SubredditScreenUiModel;
import me.saket.dank.ui.subreddits.models.SubredditScreenUiModel.SubmissionRowUiModel;
import me.saket.dank.ui.subreddits.models.SubredditSubmission;
import me.saket.dank.ui.subreddits.models.SubredditSubmissionClickEvent;
import me.saket.dank.ui.subreddits.models.SubredditSubmissionPagination;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class SubredditSubmissionsAdapter extends RecyclerViewArrayAdapter<SubmissionRowUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<SubmissionRowUiModel>, DiffUtil.DiffResult>>, InfinitelyScrollableRecyclerViewAdapter
{

  public static final int ADAPTER_ID_PAGINATION_FOOTER = -99;
  private static final SubmissionRowUiModel.Type[] VIEW_TYPES = SubmissionRowUiModel.Type.values();

  private final Map<SubmissionRowUiModel.Type, SubredditScreenUiModel.SubmissionRowUiChildAdapter> childAdapters;
  private final SubredditSubmission.Adapter submissionAdapter;

  @Inject
  public SubredditSubmissionsAdapter(SubredditSubmission.Adapter submissionAdapter, SubredditSubmissionPagination.Adapter paginationAdapter) {
    childAdapters = new HashMap<>(3);
    childAdapters.put(SubmissionRowUiModel.Type.SUBMISSION, submissionAdapter);
    childAdapters.put(SubmissionRowUiModel.Type.PAGINATION_FOOTER, paginationAdapter);

    this.submissionAdapter = submissionAdapter;
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<SubredditSubmissionClickEvent> submissionClicks() {
    return submissionAdapter.submissionClicks();
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
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    SubmissionRowUiModel item = getItem(position);
    int viewType = getItemViewType(position);
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[viewType]).onBindViewHolder(holder, item);
  }

  @Override
  public void accept(Pair<List<SubmissionRowUiModel>, DiffUtil.DiffResult> pair) throws Exception {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }
}
