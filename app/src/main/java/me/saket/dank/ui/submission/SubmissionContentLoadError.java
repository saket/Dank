package me.saket.dank.ui.submission;

import android.content.Context;

import com.google.auto.value.AutoValue;

import me.saket.dank.R;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.ui.submission.adapter.SubmissionMediaContentLoadError;

public interface SubmissionContentLoadError {

  SubmissionMediaContentLoadError.UiModel uiModel(Context context);

  @AutoValue
  abstract class LoadFailure implements SubmissionContentLoadError {

    public abstract ResolvedError resolvedError();

    public static LoadFailure create(ResolvedError error) {
      return new AutoValue_SubmissionContentLoadError_LoadFailure(error);
    }

    @Override
    public SubmissionMediaContentLoadError.UiModel uiModel(Context context) {
      return SubmissionMediaContentLoadError.UiModel.create(
          context.getString(resolvedError().errorEmojiRes()),
          context.getString(R.string.submission_media_content_load_failure_tap_to_retry),
          R.drawable.ic_error_24dp,
          this
      );
    }
  }

  @AutoValue
  abstract class NsfwContentDisabled implements SubmissionContentLoadError {
    public static NsfwContentDisabled create() {
      return new AutoValue_SubmissionContentLoadError_NsfwContentDisabled();
    }

    @Override
    public SubmissionMediaContentLoadError.UiModel uiModel(Context context) {
      return SubmissionMediaContentLoadError.UiModel.create(
          context.getString(R.string.submission_nsfw_content_disabled_error),
          context.getString(R.string.submission_tap_to_enable_nsfw_content),
          R.drawable.ic_visibility_off_24dp,
          this
      );
    }
  }
}
