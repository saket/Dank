package me.saket.dank.ui.preferences.gestures;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class GesturePreferencesAdapter extends RecyclerViewArrayAdapter<GesturePreferenceUiModel, RecyclerView.ViewHolder> {

  static final int COMMENT_PREVIEW_ADAPTER_ID = -98;
  static final int SUBMISSION_PREVIEW_ADAPTER_ID = -99;
  private static final GesturePreferenceUiModel.Type[] VIEW_TYPES = GesturePreferenceUiModel.Type.values();
  private final Map<GesturePreferenceUiModel.Type, GesturePreferenceUiModel.ChildAdapter> childAdapters;

  @Inject
  public GesturePreferencesAdapter(
      GesturePreferencesSubmissionPreview.Adapter submissionPreviewAdapter,
      GesturePreferencesSectionHeader.Adapter sectionHeaderAdapter,
      GesturePreferencesSwipeAction.Adapter swipeActionAdapter)
  {
    super();
    setHasStableIds(true);

    childAdapters = new HashMap<>();
    childAdapters.put(GesturePreferenceUiModel.Type.SUBMISSION_PREVIEW, submissionPreviewAdapter);
    childAdapters.put(GesturePreferenceUiModel.Type.SECTION_HEADER, sectionHeaderAdapter);
    childAdapters.put(GesturePreferenceUiModel.Type.SWIPE_ACTION, swipeActionAdapter);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return childAdapters.get(VIEW_TYPES[viewType]).onCreateViewHolder(inflater, parent);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position));
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    throw new UnsupportedOperationException();
  }
}
