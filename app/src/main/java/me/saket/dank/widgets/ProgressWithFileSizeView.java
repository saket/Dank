package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

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

  public ProgressWithFileSizeView(@NonNull Context context, @Nullable AttributeSet attrs) {
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

    progressBackgroundView.setMaxProgress(100);
    progressBackgroundView.setProgress(100);

    fileSizeView.setVisibility(GONE); // Set to VISIBLE when setFileSizeBytes() gets called.
    setOrientation(VERTICAL);
  }

  public void addProgressAnimationListener(CircularProgressViewListener listener) {
    progressView.addListener(listener);
  }

  public CircularProgressView getProgressView() {
    return progressView;
  }

  public void setProgress(int progress) {
    progressView.setProgress(progress);
  }

  public float getProgress() {
    return progressView.getProgress();
  }

  public void setProgressBackgroundFillEnabled(boolean enabled) {
    progressBackgroundView.setVisibility(enabled ? VISIBLE : GONE);
  }

  public void setIndeterminate(boolean indeterminate) {
    progressView.setIndeterminate(indeterminate);
    progressView.startAnimation();
  }

  public void setFileSizeBytes(double size, FileSizeUnit sizeUnit) {
    if (lastFileSize == size && lastFileSizeUnit == sizeUnit) {
      return;
    }
    lastFileSize = size;
    lastFileSizeUnit = sizeUnit;

    @StringRes int stringTemplateInSensibleUnitRes = R.string.filesize_gigabytes;
    double sizeInSensibleUnit = sizeUnit.toGigaBytes(size);

    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_megabytes;
      sizeInSensibleUnit = sizeUnit.toMegaBytes(size);
    }
    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_kilobytes;
      sizeInSensibleUnit = sizeUnit.toKiloBytes(size);
    }
    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_bytes;
      sizeInSensibleUnit = sizeUnit.toBytes(size);
    }

    fileSizeView.setVisibility(VISIBLE);
    fileSizeView.setText(fileSizeView.getResources().getString(stringTemplateInSensibleUnitRes, (int) sizeInSensibleUnit));
  }
}
