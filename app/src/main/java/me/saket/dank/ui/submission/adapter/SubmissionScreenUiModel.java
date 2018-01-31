package me.saket.dank.ui.submission.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

/**
 * Also see {@link SubmissionContentLinkUiModel}, which does not implement this interface.
 */
public interface SubmissionScreenUiModel {

  long adapterId();

  SubmissionCommentRowType type();

  interface Adapter<T extends SubmissionScreenUiModel, VH extends RecyclerView.ViewHolder> {
    VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);

    void onBindViewHolder(VH holder, T uiModel);

    void onBindViewHolder(VH holder, T uiModel, List<Object> payloads);

    default void onViewRecycled(VH holder) {
      // Optional.
    }
  }
}
