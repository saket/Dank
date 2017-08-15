package me.saket.dank.notifs;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.ui.media.MediaDownloadJob;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.glide.GlideProgressTarget;
import timber.log.Timber;

/**
 * Downloads images and videos to disk.
 */
public class MediaDownloadService extends Service {

  private static final String KEY_MEDIA_LINK_TO_DOWNLOAD = "mediaLinkToDownload";
  private static final int MAX_LENGTH_FOR_NOTIFICATION_TITLE = 40;
  private static final int P_INTENT_REQ_CODE_RETRY_DOWNLOAD = 300;

  private final Set<String> ongoingDownloadUrls = new HashSet<>();
  private final Relay<MediaLink> mediaLinksToDownloadStream = PublishRelay.create();
  private Disposable downloadDisposable = Disposables.disposed();

  private final Map<String, MediaDownloadJob> downloadJobs = new HashMap<>();
  private final Relay<Collection<MediaDownloadJob>> downloadJobStream = PublishRelay.create();

  public static void enqueueDownload(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_DOWNLOAD, mediaLink);
    context.startService(intent);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    downloadJobStream.subscribe(downloadJobs -> {
      boolean allMediaDownloaded = true;
      for (MediaDownloadJob downloadJob : downloadJobs) {
        if (downloadJob.progressState() != MediaDownloadJob.ProgressState.DOWNLOADED) {
          allMediaDownloaded = false;
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Notification bundleSummaryNotification = createOrUpdateBundleSummaryNotification(downloadJobs, allMediaDownloaded);
        NotificationManagerCompat.from(this).notify(NotificationIds.MEDIA_DOWNLOAD_BUNDLE_SUMMARY, bundleSummaryNotification);
      }

      if (allMediaDownloaded) {
        stopSelf();
      }
    });

    downloadDisposable = mediaLinksToDownloadStream
        .flatMap(mediaLink -> downloadImage(mediaLink).doOnComplete(() -> ongoingDownloadUrls.remove(mediaLink.originalUrl())))
        .subscribe(ongoingDownloadJob -> {
          int notificationId = (NotificationIds.MEDIA_DOWNLOAD_INDIVIDUAL_PROGRESS_PREFIX_ + ongoingDownloadJob.mediaLink().originalUrl()).hashCode();

          switch (ongoingDownloadJob.progressState()) {
            case CONNECTING:
              updateIndividualProgressNotification(ongoingDownloadJob, notificationId);
              break;

            case IN_FLIGHT:
              updateIndividualProgressNotification(ongoingDownloadJob, notificationId);
              break;

            case FAILED:
              displayErrorNotification(ongoingDownloadJob, notificationId);
              ongoingDownloadUrls.remove(ongoingDownloadJob.mediaLink().originalUrl());
              break;

            case DOWNLOADED:
              // TODO: Show media preview notif.
              NotificationManagerCompat.from(this).cancel(notificationId);
              ongoingDownloadUrls.remove(ongoingDownloadJob.mediaLink().originalUrl());
              break;
          }

          downloadJobs.put(ongoingDownloadJob.mediaLink().originalUrl(), ongoingDownloadJob);
          downloadJobStream.accept(downloadJobs.values());
        });
  }

  @Override
  public void onDestroy() {
    downloadDisposable.dispose();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    MediaLink mediaLinkToDownload = (MediaLink) intent.getSerializableExtra(KEY_MEDIA_LINK_TO_DOWNLOAD);
    boolean duplicateFound = ongoingDownloadUrls.contains(mediaLinkToDownload.originalUrl());

    // TODO: Remove.
    if (duplicateFound) {
      int notificationId = (NotificationIds.MEDIA_DOWNLOAD_INDIVIDUAL_PROGRESS_PREFIX_ + mediaLinkToDownload.originalUrl()).hashCode();
      NotificationManagerCompat.from(this).cancel(notificationId);
      duplicateFound = false;
    }

    if (!duplicateFound) {
      ongoingDownloadUrls.add(mediaLinkToDownload.originalUrl());
      mediaLinksToDownloadStream.accept(mediaLinkToDownload);
    }
    return START_NOT_STICKY;
  }

  @TargetApi(Build.VERSION_CODES.N)
  private Notification createOrUpdateBundleSummaryNotification(Collection<MediaDownloadJob> downloadJobs, boolean isCancelable) {
    return new NotificationCompat.Builder(this)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setGroup(NotificationIds.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setGroupSummary(true)
        .setShowWhen(true)
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setOnlyAlertOnce(true)
        .setWhen(0)
        .setOngoing(!isCancelable)
        .build();
  }

  private void updateIndividualProgressNotification(MediaDownloadJob mediaDownloadJob, int notificationId) {
    String urlWithoutScheme = ellipsizeNotifTitleIfExceedsMaxLength(mediaDownloadJob.mediaLink().originalUrl());
    String notificationTitle = "Saving " + urlWithoutScheme;
    boolean indeterminateProgress = mediaDownloadJob.progressState() == MediaDownloadJob.ProgressState.CONNECTING;

    Notification notification = new NotificationCompat.Builder(this)
        .setContentTitle(notificationTitle)
        .setContentText(mediaDownloadJob.downloadProgress() + "%")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .setGroup(NotificationIds.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setLocalOnly(true)   // Hide from wearables.
        .setWhen(0)
        .setProgress(100 /* max */, mediaDownloadJob.downloadProgress(), indeterminateProgress)
        .build();

    NotificationManagerCompat.from(this).notify(notificationId, notification);
  }

  private static String ellipsizeNotifTitleIfExceedsMaxLength(String fullTitle) {
    return fullTitle.length() > MAX_LENGTH_FOR_NOTIFICATION_TITLE
        ? Strings.safeSubstring(fullTitle, MAX_LENGTH_FOR_NOTIFICATION_TITLE) + "…"
        : fullTitle;
  }

  private void displayErrorNotification(MediaDownloadJob failedDownloadJob, int notificationId) {
    int requestId = P_INTENT_REQ_CODE_RETRY_DOWNLOAD + notificationId;
    Intent retryIntent = MediaNotifActionReceiver.createRetryDownloadIntent(this, failedDownloadJob);
    PendingIntent retryPendingIntent = PendingIntent.getBroadcast(this, requestId, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification errorNotification = new NotificationCompat.Builder(this)
        .setContentTitle(getString(
            failedDownloadJob.mediaLink().isVideo()
                ? R.string.mediadownloadnotification_failed_to_save_video
                : R.string.mediadownloadnotification_failed_to_save_image
        ))
        .setContentText(getString(R.string.mediadownloadnotification_tap_to_retry_url, failedDownloadJob.mediaLink().originalUrl()))
        .setSmallIcon(R.drawable.ic_error_24dp)
        .setOngoing(false)
        .setGroup(NotificationIds.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setLocalOnly(true)   // Hide from wearables.
        .setWhen(0) // TODO: set this.
        .setContentIntent(retryPendingIntent)
        .setAutoCancel(false)
        .build();
    NotificationManagerCompat.from(this).notify(notificationId, errorNotification);
  }

  private void displaySuccessNotification(MediaDownloadJob completedDownloadJob, int notificationId) {

  }

  // TODO: Remove random url.
  private Observable<MediaDownloadJob> downloadImage(MediaLink mediaLink) throws ExecutionException, InterruptedException {
    String imageUrl = mediaLink.originalUrl() + "?" + String.valueOf(System.currentTimeMillis());
//    String imageUrl = "";
//    Timber.i("Image url: %s", imageUrl);

    return Observable.create(emitter -> {
      Target<File> fileFutureTarget = new SimpleTarget<File>() {
        @Override
        public void onResourceReady(File downloadedFile, Transition<? super File> transition) {
          Timber.i("onResourceReady()");
          emitter.onNext(MediaDownloadJob.createProgress(mediaLink, 100));
          emitter.onNext(MediaDownloadJob.createDownloaded(mediaLink, downloadedFile));
          emitter.onComplete();
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
          emitter.onNext(MediaDownloadJob.createFailed(mediaLink));
          emitter.onComplete();
        }
      };

      GlideProgressTarget<String, File> progressTarget = new GlideProgressTarget<String, File>(fileFutureTarget) {
        @Override
        public float getGranularityPercentage() {
          return 0.5f;
        }

        @Override
        protected void onConnecting() {
          emitter.onNext(MediaDownloadJob.createConnecting(mediaLink));
        }

        @Override
        protected void onDownloading(long bytesRead, long expectedLength) {
          int progress = (int) (100 * (float) bytesRead / expectedLength);
          emitter.onNext(MediaDownloadJob.createProgress(mediaLink, progress));
        }

        @Override
        protected void onDownloaded() {}

        @Override
        protected void onDelivered() {}
      };
      progressTarget.setModel(this, imageUrl);

      Glide.with(this)
          .download(imageUrl)
          .into(progressTarget);

      emitter.setCancellable(() -> Glide.with(this).clear(progressTarget));
    });
  }
}
