package me.saket.dank.data;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * @param <T> Successful response type.
 */
@AutoValue
public abstract class FileUploadProgressEvent<T> {

  @FloatRange(from = 0f, to = 1f)
  public abstract Float progress();

  @Nullable
  public abstract T uploadResponse();

  public boolean isInFlight() {
    return uploadResponse() == null;
  }

  public boolean isUploaded() {
    return uploadResponse() != null;
  }

  public static <T> FileUploadProgressEvent<T> createInFlight(@FloatRange(from = 0f, to = 1f) Float progress) {
    return new AutoValue_FileUploadProgressEvent<>(progress, null);
  }

  public static <T> FileUploadProgressEvent<T> createUploaded(T uploadResponse) {
    return new AutoValue_FileUploadProgressEvent<>(100f, uploadResponse);
  }
}
