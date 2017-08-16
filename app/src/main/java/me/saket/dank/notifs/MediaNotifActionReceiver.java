package me.saket.dank.notifs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;

import me.saket.dank.R;
import me.saket.dank.ui.media.MediaDownloadJob;
import me.saket.dank.utils.Intents;

public class MediaNotifActionReceiver extends BroadcastReceiver {

  private static final String KEY_DOWNLOAD_JOB = "downloadJob";
  private static final String KEY_ACTION = "action";

  enum Action {
    RETRY_DOWNLOAD,
    SHARE_IMAGE,
    DELETE_IMAGE,
    NOTIFICATION_DISMISSED,
  }

  public static Intent createRetryDownloadIntent(Context context, MediaDownloadJob failedDownloadJob) {
    Intent intent = new Intent(context, MediaNotifActionReceiver.class);
    intent.putExtra(KEY_DOWNLOAD_JOB, failedDownloadJob);
    intent.putExtra(KEY_ACTION, Action.RETRY_DOWNLOAD);
    intent.setClass(context, MediaNotifActionReceiver.class);
    return intent;
  }

  public static Intent createShareImageIntent(Context context, MediaDownloadJob downloadJob) {
    Intent intent = new Intent(context, MediaNotifActionReceiver.class);
    intent.putExtra(KEY_DOWNLOAD_JOB, downloadJob);
    intent.putExtra(KEY_ACTION, Action.SHARE_IMAGE);
    intent.setClass(context, MediaNotifActionReceiver.class);
    return intent;
  }

  public static Intent createDeleteImageIntent(Context context, MediaDownloadJob downloadJob) {
    Intent intent = new Intent(context, MediaNotifActionReceiver.class);
    intent.putExtra(KEY_DOWNLOAD_JOB, downloadJob);
    intent.putExtra(KEY_ACTION, Action.DELETE_IMAGE);
    intent.setClass(context, MediaNotifActionReceiver.class);
    return intent;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    MediaDownloadJob downloadJob = intent.getParcelableExtra(KEY_DOWNLOAD_JOB);

    switch (((Action) intent.getSerializableExtra(KEY_ACTION))) {
      case RETRY_DOWNLOAD:
        MediaDownloadService.enqueueDownload(context, downloadJob.mediaLink());
        break;

      case SHARE_IMAGE:
        Uri imageContentUri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), downloadJob.downloadedFile());
        Intent shareIntent = Intents.createForSharingImage(context, imageContentUri);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.mediaalbumviewer_share_sheet_title)));
        dismissNotification(context, downloadJob);
        break;

      case DELETE_IMAGE:
        //noinspection ConstantConditions
        boolean deleted = downloadJob.downloadedFile().delete();
        if (!deleted) {
          throw new AssertionError("Couldn't delete image: " + downloadJob.downloadedFile());
        }
        dismissNotification(context, downloadJob);
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  /**
   * We should ideally be asking MediaDownloadService for dismissing the notification, but whatever.
   */
  private void dismissNotification(Context context, MediaDownloadJob downloadJob) {
    NotificationManagerCompat.from(context).cancel(MediaDownloadService.createNotificationIdFor(downloadJob));
  }
}
