package me.saket.dank.widgets;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.github.rahatarmanahmed.cpv.CircularProgressViewListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.FileSizeUnit;
import me.saket.dank.utils.Views;

public class ProgressWithFileSizeView extends LinearLayout {

  @BindView(R.id.progressbarwithfilesize_progress_fill) CircularProgressView progressBackgroundView;
  @BindView(R.id.progressbarwithfilesize_progress) CircularProgressView progressView;
  @BindView(R.id.progressbarwithfilesize_file_size) TextView fileSizeView;

  private double lastFileSize;
  private FileSizeUnit lastFileSizeUnit;

  public ProgressWithFileSizeView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.custom_progress_bar_with_file_size, this, true);
    ButterKnife.bind(this, this);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ProgressWithFileSizeView);
    if (attributes.hasValue(R.styleable.ProgressWithFileSizeView_progressBarSize)) {
      int progressBarSize = attributes.getDimensionPixelSize(R.styleable.ProgressWithFileSizeView_progressBarSize, 0);
      Views.setDimensions(progressBackgroundView, progressBarSize, progressBarSize);
      Views.setDimensions(progressView, progressBarSize, progressBarSize);
    }
    attributes.recycle();

    setLayoutTransition(new LayoutTransition());
    progressBackgroundView.setMaxProgress(100);
    progressBackgroundView.setProgress(100);

    fileSizeView.setVisibility(INVISIBLE); // Set to VISIBLE when setFileSizeBytes() gets called.
    setOrientation(VERTICAL);

    if (isInEditMode()) {
      setProgress(50f);
      setFileSize(1.3, FileSizeUnit.MB);
    }
  }

  public void addProgressAnimationListener(CircularProgressViewListener listener) {
    progressView.addListener(listener);
  }

  public void setProgress(@FloatRange(from = 0, to = 100) float progress) {
    progressView.setProgress(progress);
  }

  @FloatRange(from = 0, to = 100)
  public float getProgress() {
    return progressView.getProgress();
  }

  /**
   * Toggle background for the progress circle.
   */
  public void setProgressBarBackgroundFillEnabled(boolean enabled) {
    progressBackgroundView.setVisibility(enabled ? VISIBLE : GONE);
  }

  public void setIndeterminate(boolean indeterminate) {
    progressView.setIndeterminate(indeterminate);
    progressView.startAnimation();
  }

  public void setFileSize(double size, FileSizeUnit sizeUnit) {
    if (lastFileSize == size && lastFileSizeUnit == sizeUnit) {
      return;
    }
    lastFileSize = size;
    lastFileSizeUnit = sizeUnit;

    String formattedSize = FileSizeUnit.formatForDisplay(fileSizeView.getResources(), size, sizeUnit);
    fileSizeView.setVisibility(VISIBLE);
    fileSizeView.setText(formattedSize);
  }
}
