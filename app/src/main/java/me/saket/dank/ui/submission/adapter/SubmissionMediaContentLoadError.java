package me.saket.dank.ui.submission.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.List;

import javax.inject.Inject;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SubmissionContentLoadError;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.AnimatedProgressBar;

public interface SubmissionMediaContentLoadError {

  Object NOTHING = LifecycleStreams.NOTHING;

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    public abstract String title();

    public abstract String byline();

    @DrawableRes
    public abstract int errorIconRes();

    public abstract SubmissionContentLoadError error();

    @Override
    public long adapterId() {
      return SubmissionCommentsAdapter.ID_MEDIA_CONTENT_LOAD_ERROR;
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.MEDIA_CONTENT_LOAD_ERROR;
    }

    public static UiModel create(String title, String byline, int errorIconRes, SubmissionContentLoadError error) {
      return new AutoValue_SubmissionMediaContentLoadError_UiModel(title, byline, errorIconRes, error);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final ViewGroup containerView;
    private final TextView titleView;
    private final TextView bylineView;
    private final ImageView iconView;
    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_header_link, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_link_title);
      bylineView = itemView.findViewById(R.id.submission_link_byline);
      iconView = itemView.findViewById(R.id.submission_link_icon);
      containerView = itemView.findViewById(R.id.submission_link_container);

      titleView.setMaxLines(2);
      bylineView.setMaxLines(2);
      containerView.setClipToOutline(true);

      ((AnimatedProgressBar) itemView.findViewById(R.id.submission_link_progress)).setVisibilityWithoutAnimation(View.GONE);
    }

    public void setupClicks(Relay<SubmissionContentLoadError> clickStream) {
      itemView.setOnClickListener(o -> clickStream.accept(uiModel.error()));
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    private void render() {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());
      iconView.setImageResource(uiModel.errorIconRes());
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    final Relay<SubmissionContentLoadError> mediaContentLoadRetryClickStream = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.setupClicks(mediaContentLoadRetryClickStream);
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.render();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
