package me.saket.dank.ui.media;

import android.os.Parcelable;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import me.saket.dank.data.MediaLink;

@AutoValue
public abstract class MediaDownloadJob implements Parcelable {

  public enum ProgressState {
    CONNECTING,
    IN_FLIGHT,
    FAILED,
    DOWNLOADED
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

  public static MediaDownloadJob createConnecting(MediaLink mediaLink) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.CONNECTING, 0, null);
  }

  public static MediaDownloadJob createProgress(MediaLink mediaLink, @IntRange(from = 0, to = 100) int progress) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.IN_FLIGHT, progress, null);
  }

  public static MediaDownloadJob createFailed(MediaLink mediaLink) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.FAILED, -1, null);
  }

  public static MediaDownloadJob createDownloaded(MediaLink mediaLink, File downloadedFile) {
    return new AutoValue_MediaDownloadJob(mediaLink, ProgressState.DOWNLOADED, 100, downloadedFile);
  }
}
