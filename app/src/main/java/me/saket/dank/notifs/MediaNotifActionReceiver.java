package me.saket.dank.notifs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.saket.dank.ui.media.MediaDownloadJob;

public class MediaNotifActionReceiver extends BroadcastReceiver {

  private static final String KEY_FAILED_DOWNLOAD_JOB = "failedDownloadJob";
  private static final String KEY_ACTION = "action";

  enum Action {
    RETRY_DOWNLOAD,
    NOTIFICATION_DISMISSED
  }

  public static Intent createRetryDownloadIntent(Context context, MediaDownloadJob failedDownloadJob) {
    Intent intent = new Intent(context, MediaNotifActionReceiver.class);
    intent.putExtra(KEY_FAILED_DOWNLOAD_JOB, failedDownloadJob);
    intent.putExtra(KEY_ACTION, Action.RETRY_DOWNLOAD);
    return intent;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    switch (((Action) intent.getSerializableExtra(KEY_ACTION))) {
      case RETRY_DOWNLOAD:
        MediaDownloadJob failedDownloadJob = intent.getParcelableExtra(KEY_FAILED_DOWNLOAD_JOB);
        MediaDownloadService.enqueueDownload(context, failedDownloadJob.mediaLink());
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }
}
