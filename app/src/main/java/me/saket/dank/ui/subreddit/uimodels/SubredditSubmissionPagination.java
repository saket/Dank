package me.saket.dank.ui.subreddit.uimodels;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.subreddit.SubredditSubmissionsAdapter;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

public interface SubredditSubmissionPagination {

  Object NOTHING = LifecycleStreams.NOTHING;

  @AutoValue
  abstract class UiModel implements SubredditScreenUiModel.SubmissionRowUiModel {
    @Override
    public long adapterId() {
      return SubredditSubmissionsAdapter.ADAPTER_ID_PAGINATION_FOOTER;
    }

    @Override
    public Type type() {
      return Type.PAGINATION_FOOTER;
    }

    public abstract boolean progressVisible();

    public abstract Optional<Integer> errorTextRes();

    public static UiModel create(boolean progressVisible, Optional<Integer> errorTextRes) {
      return new AutoValue_SubredditSubmissionPagination_UiModel(progressVisible, errorTextRes);
    }

    public static UiModel createProgress() {
      return create(true, Optional.empty());
    }

    public static UiModel createError(Integer errorTextRes) {
      return create(false, Optional.of(errorTextRes));
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final View progressView;
    private final TextView errorTextView;

    public ViewHolder(View itemView) {
      super(itemView);
      progressView = itemView.findViewById(R.id.infinitescroll_footer_progress);
      errorTextView = itemView.findViewById(R.id.infinitescroll_footer_error);
    }

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_subreddit_pagination_footer, parent, false));
    }

    public void render(UiModel uiModel) {
      progressView.setVisibility(uiModel.progressVisible() ? View.VISIBLE : View.GONE);
      errorTextView.setVisibility(uiModel.errorTextRes().isPresent() ? View.VISIBLE : View.GONE);
      if (uiModel.errorTextRes().isPresent()) {
        errorTextView.setText(uiModel.errorTextRes().get());
      }
    }
  }

  class Adapter implements SubredditScreenUiModel.SubmissionRowUiChildAdapter<UiModel, ViewHolder> {
    private Relay<Object> retryClicks = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.itemView.setOnClickListener(o -> {
        retryClicks.accept(NOTHING);
      });
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.render(uiModel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }

    @CheckResult
    public Observable<?> failureRetryClicks() {
      return retryClicks;
    }
  }
}
