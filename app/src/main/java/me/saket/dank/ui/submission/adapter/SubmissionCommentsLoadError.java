package me.saket.dank.ui.submission.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import me.saket.dank.R;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.ErrorStateView;

public interface SubmissionCommentsLoadError {

  Object NOTHING = LifecycleStreams.NOTHING;

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    public abstract ResolvedError resolvedError();

    @Override
    public long adapterId() {
      return SubmissionCommentsAdapter.ID_COMMENTS_LOAD_ERROR;
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.COMMENTS_LOAD_ERROR;
    }

    public static UiModel create(ResolvedError resolvedError) {
      return new AutoValue_SubmissionCommentsLoadError_UiModel(resolvedError);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final ErrorStateView errorStateView;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_load_error, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      errorStateView = ((ErrorStateView) itemView);
    }

    public void setupRetryClicks(Relay<Object> retryClickStream) {
      errorStateView.setOnRetryClickListener(o -> retryClickStream.accept(NOTHING));
    }

    public void bind(UiModel model) {
      errorStateView.applyFrom(model.resolvedError());
    }
  }
}
