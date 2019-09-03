package me.saket.dank.ui.submission.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.reactivex.Observable;
import me.saket.dank.ui.UiEvent;

/**
 * Also see {@link SubmissionContentLinkUiModel}, which does not implement this interface.
 */
public interface SubmissionScreenUiModel {

  long adapterId();

  SubmissionCommentRowType type();

  interface Adapter<T extends SubmissionScreenUiModel, VH extends RecyclerView.ViewHolder> {

    // TODO: Use this for all adapter events.
    default Observable<? extends UiEvent> uiEvents() {
      return Observable.empty();
    }

    VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);

    void onBindViewHolder(VH holder, T uiModel);

    void onBindViewHolder(VH holder, T uiModel, List<Object> payloads);

    default void onViewRecycled(VH holder) {
      // Optional.
    }
  }
}
