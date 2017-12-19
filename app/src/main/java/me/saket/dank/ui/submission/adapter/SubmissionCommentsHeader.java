package me.saket.dank.ui.submission.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import me.saket.dank.R;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Optional;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionCommentsHeader {

  static int getWidthForAlbumContentLinkThumbnail(Context context) {
    return context.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract CharSequence title();

    public abstract CharSequence byline();

    public abstract Optional<CharSequence> selfText();

    public abstract Optional<ContentLinkUiModel> contentLink();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.SUBMISSION_HEADER;
    }

    public static UiModel create(
        long adapterId,
        CharSequence title,
        CharSequence byline,
        Optional<CharSequence> selfText,
        Optional<ContentLinkUiModel> contentLink)
    {
      return new AutoValue_SubmissionCommentsHeader_UiModel(adapterId, title, byline, selfText, contentLink);
    }
  }

  @AutoValue
  abstract class ContentLinkUiModel {
    public abstract CharSequence title();

    public abstract CharSequence byline();

    @Nullable
    public abstract Bitmap icon();

    @Nullable
    public abstract Bitmap thumbnail();

    public abstract int titleMaxLines();

    public abstract boolean progressVisible();

    public static ContentLinkUiModel.Builder builder() {
      return new AutoValue_SubmissionCommentsHeader_ContentLinkUiModel.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder title(CharSequence title);

      public abstract Builder byline(CharSequence byline);

      public abstract Builder icon(Bitmap icon);

      public abstract Builder thumbnail(Bitmap thumbnail);

      public abstract Builder titleMaxLines(int maxLines);

      public abstract Builder progressVisible(boolean visible);

      public abstract ContentLinkUiModel build();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private TextView titleView;
    private TextView bylineView;
    private final TextView selfTextView;
    private final ViewGroup contentLinkView;
    private final ImageView contentLinkIconView;
    private final ImageView contentLinkThumbnailView;
    private final TextView contentLinkTitleView;
    private final TextView contentLinkBylineView;
    private final ProgressBar contentLinkProgressView;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_header, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_title);
      bylineView = itemView.findViewById(R.id.submission_byline);
      selfTextView = itemView.findViewById(R.id.submission_selfpost_text);
      contentLinkView = itemView.findViewById(R.id.submission_link_container);
      contentLinkIconView = itemView.findViewById(R.id.submission_link_icon);
      contentLinkThumbnailView = itemView.findViewById(R.id.submission_link_thumbnail);
      contentLinkTitleView = itemView.findViewById(R.id.submission_link_title);
      contentLinkBylineView = itemView.findViewById(R.id.submission_link_byline);
      contentLinkProgressView = itemView.findViewById(R.id.submission_link_progress);
    }

    public void bind(UiModel uiModel, DankLinkMovementMethod movementMethod, Relay<Object> headerClickStream) {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());

      // TODO.
      selfTextView.setVisibility(View.GONE);
      selfTextView.setMovementMethod(movementMethod);

      if (uiModel.contentLink().isPresent()) {
        ContentLinkUiModel contentLinkUiModel = uiModel.contentLink().get();
        contentLinkView.setVisibility(View.VISIBLE);

        contentLinkTitleView.setText(contentLinkUiModel.title());
        contentLinkBylineView.setText(contentLinkUiModel.byline());
        contentLinkProgressView.setVisibility(contentLinkUiModel.progressVisible() ? View.VISIBLE : View.GONE);
        if (contentLinkUiModel.icon() != null) {
          contentLinkIconView.setImageBitmap(contentLinkUiModel.icon());
        }
        if (contentLinkUiModel.thumbnail() != null) {
          contentLinkThumbnailView.setImageBitmap(contentLinkUiModel.thumbnail());
        }

      } else {
        contentLinkView.setVisibility(View.GONE);
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
