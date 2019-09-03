package me.saket.dank.ui.media;

import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import me.saket.dank.urlparser.MediaLink;

@AutoValue
public abstract class MediaDownloadJob implements Parcelable {

  public enum ProgressState {
    QUEUED,
    CONNECTING,
    IN_FLIGHT,
    FAILED,
    DOWNLOADED,
  }

  public abstract MediaLink mediaLink();

  public abstract ProgressState progressState();

  @IntRange(from = 0, to = 100)
  public abstract int downloadProgress();

  /**
   * Null until the file is downloaded.
   */
  @Nullable
  public abstract File downloadedFile();

  public abstract long timestamp();

  public static MediaDownloadJob queued(MediaLink mediaLink, long queueTimeMillis) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.QUEUED, 0, null, queueTimeMillis);
  }

  public static MediaDownloadJob connecting(MediaLink mediaLink, long startTimeMillis) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.CONNECTING, 0, null, startTimeMillis);
  }

  public static MediaDownloadJob progress(MediaLink mediaLink, @IntRange(from = 0, to = 100) int progress, long startTimeMillis) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.IN_FLIGHT, progress, null, startTimeMillis);
  }

  public static MediaDownloadJob failed(MediaLink mediaLink, long failTimeMillis) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.FAILED, -1, null, failTimeMillis);
  }

  public static MediaDownloadJob downloaded(MediaLink mediaLink, File downloadedFile, long completeTimeMillis) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.DOWNLOADED, 100, downloadedFile, completeTimeMillis);
  }
}
