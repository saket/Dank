package me.saket.dank.notifs;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.ui.media.MediaDownloadJob;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.glide.GlideProgressTarget;
import timber.log.Timber;

/**
 * Downloads images and videos to disk.
 */
public class MediaDownloadService extends Service {

  private static final String KEY_MEDIA_LINK_TO_DOWNLOAD = "mediaLinkToDownload";
  private static final String KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD = "mediaLinkToCancelDownload";
  private static final String KEY_MEDIA_LINK_TO_DISMISS_NOTIF = "mediaLinkToDismissNotif";
  private static final String KEY_ACTION = "action";
  private static final int MAX_LENGTH_FOR_NOTIFICATION_TITLE = 40;
  private static final String REQUESTCODE_RETRY_DOWNLOAD_PREFIX_ = "300";
  private static final String REQUESTCODE_SHARE_IMAGE_PREFIX_ = "301";
  private static final String REQUESTCODE_DELETE_IMAGE_PREFIX_ = "302";
  private static final String REQUESTCODE_OPEN_IMAGE_PREFIX_ = "303";
  private static final String REQUESTCODE_CANCEL_DOWNLOAD_PREFIX_ = "304";

  private final Set<String> ongoingDownloadUrls = new HashSet<>();
  private final Relay<MediaLink> mediaLinksToDownloadStream = PublishRelay.create();
  private CompositeDisposable disposables = new CompositeDisposable();
  private final Map<String, MediaDownloadJob> downloadJobs = new HashMap<>();
  private final Relay<Collection<MediaDownloadJob>> downloadJobsCombinedProgressChangeStream = PublishRelay.create();
  private final Relay<MediaLink> downloadCancellationStream = PublishRelay.create();

  enum Action {
    ENQUEUE_DOWNLOAD,
    CANCEL_DOWNLOAD,
    DISMISS_NOTIFICATION,
  }

  public static void enqueueDownload(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_DOWNLOAD, mediaLink);
    intent.putExtra(KEY_ACTION, Action.ENQUEUE_DOWNLOAD);
    context.startService(intent);
  }

  public static Intent createCancelDownloadIntent(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD, mediaLink);
    intent.putExtra(KEY_ACTION, Action.CANCEL_DOWNLOAD);
    return intent;
  }

  public static void dismissNotification(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_DISMISS_NOTIF, mediaLink);
    intent.putExtra(KEY_ACTION, Action.DISMISS_NOTIFICATION);
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

    // Create a summary notification + stop service when all downloads finish.
    long startTimeMillis = System.currentTimeMillis();
    disposables.add(
        downloadJobsCombinedProgressChangeStream
            .map(downloadJobs -> {
              boolean allMediaDownloaded = true;
              for (MediaDownloadJob downloadJob : downloadJobs) {
                if (downloadJob.progressState() != MediaDownloadJob.ProgressState.DOWNLOADED) {
                  allMediaDownloaded = false;
                }
              }
              return allMediaDownloaded;
            })
            .distinctUntilChanged()
            .subscribe(allMediaDownloaded -> {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Notification bundleSummaryNotification = createOrUpdateBundleSummaryNotification(allMediaDownloaded, startTimeMillis);
                NotificationManagerCompat.from(this).notify(NotificationConstants.MEDIA_DOWNLOAD_BUNDLE_SUMMARY, bundleSummaryNotification);
              }

              if (allMediaDownloaded) {
                stopSelf();
              }
            })
    );

    disposables.add(
        mediaLinksToDownloadStream
            .flatMap(linkToDownload -> downloadImage(linkToDownload)
                .takeUntil(downloadCancellationStream.filter(linkToCancel -> linkToCancel.equals(linkToDownload)))
                .doOnTerminate(() -> ongoingDownloadUrls.remove(linkToDownload.originalUrl()))
            )
            // Starting from Nougat, Android has a rate limiter in place which puts a certain
            // threshold between every update. The exact value is somewhere between 100ms to 200ms.
            .sample(200, TimeUnit.MILLISECONDS, true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(downloadJob -> {
              int notificationId = createNotificationIdFor(downloadJob.mediaLink());

              switch (downloadJob.progressState()) {
                case CONNECTING:
                  updateIndividualProgressNotification(downloadJob, notificationId);
                  break;

                case IN_FLIGHT:
                  updateIndividualProgressNotification(downloadJob, notificationId);
                  break;

                case FAILED:
                  displayErrorNotification(downloadJob, notificationId);
                  ongoingDownloadUrls.remove(downloadJob.mediaLink().originalUrl());
                  break;

                case DOWNLOADED:
                  displaySuccessNotification(downloadJob, notificationId);
                  ongoingDownloadUrls.remove(downloadJob.mediaLink().originalUrl());
                  break;
              }

              downloadJobs.put(downloadJob.mediaLink().originalUrl(), downloadJob);
              downloadJobsCombinedProgressChangeStream.accept(downloadJobs.values());
            })
    );
  }

  @Override
  public void onDestroy() {
    disposables.clear();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Action serviceAction = (Action) intent.getSerializableExtra(KEY_ACTION);

    switch (serviceAction) {
      case ENQUEUE_DOWNLOAD:
        MediaLink mediaLinkToDownload = (MediaLink) intent.getSerializableExtra(KEY_MEDIA_LINK_TO_DOWNLOAD);
        boolean duplicateFound = ongoingDownloadUrls.contains(mediaLinkToDownload.originalUrl());

        // TODO: Remove.
        if (duplicateFound) {
          int notificationId = (NotificationConstants.MEDIA_DOWNLOAD_PROGRESS_PREFIX_ + mediaLinkToDownload.originalUrl()).hashCode();
          NotificationManagerCompat.from(this).cancel(notificationId);
          duplicateFound = false;
        }

        if (!duplicateFound) {
          ongoingDownloadUrls.add(mediaLinkToDownload.originalUrl());
          mediaLinksToDownloadStream.accept(mediaLinkToDownload);
        }
        break;

      case CANCEL_DOWNLOAD:
        MediaLink mediaLinkToCancel = (MediaLink) intent.getSerializableExtra(KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD);
        downloadCancellationStream.accept(mediaLinkToCancel);
        NotificationManagerCompat.from(this).cancel(createNotificationIdFor(mediaLinkToCancel));
        break;

      case DISMISS_NOTIFICATION:
        MediaLink mediaLinkToDismissNotif = (MediaLink) intent.getSerializableExtra(KEY_MEDIA_LINK_TO_DISMISS_NOTIF);
        NotificationManagerCompat.from(this).cancel(createNotificationIdFor(mediaLinkToDismissNotif));
        break;

      default:
        throw new UnsupportedOperationException("Unknown action: " + serviceAction);
    }
    return START_NOT_STICKY;
  }

  @TargetApi(Build.VERSION_CODES.N)
  private Notification createOrUpdateBundleSummaryNotification(boolean isCancelable, long startTimeMillis) {
    return new NotificationCompat.Builder(this)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setGroup(NotificationConstants.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setGroupSummary(true)
        .setShowWhen(true)
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setOnlyAlertOnce(true)
        .setWhen(startTimeMillis)
        .build();
  }

  private void updateIndividualProgressNotification(MediaDownloadJob mediaDownloadJob, int notificationId) {
    String notificationTitle = ellipsizeNotifTitleIfExceedsMaxLength(getString(
        R.string.mediaalbumviewer_download_notification_title,
        mediaDownloadJob.mediaLink().originalUrl()
    ));
    boolean indeterminateProgress = mediaDownloadJob.progressState() == MediaDownloadJob.ProgressState.CONNECTING;

    NotificationCompat.Action cancelAction = new NotificationCompat.Action(0,
        getString(R.string.mediadownloadnotification_cancel),
        PendingIntent.getService(this,
            createRequestId(REQUESTCODE_CANCEL_DOWNLOAD_PREFIX_, notificationId),
            createCancelDownloadIntent(this, mediaDownloadJob.mediaLink()),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    );

    Notification notification = new NotificationCompat.Builder(this)
        .setContentTitle(notificationTitle)
        .setContentText(mediaDownloadJob.downloadProgress() + "%")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .setGroup(NotificationConstants.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setLocalOnly(true)   // Hide from wearables.
        .setWhen(0)
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        .setProgress(100 /* max */, mediaDownloadJob.downloadProgress(), indeterminateProgress)
        .addAction(cancelAction)
        .build();

    NotificationManagerCompat.from(this).notify(notificationId, notification);
  }

  /**
   * We're ellipsizing the title so that the notification's content text (which is the progress
   * percentage at the time of writing this) is always visible.
   */
  private static String ellipsizeNotifTitleIfExceedsMaxLength(String fullTitle) {
    return fullTitle.length() > MAX_LENGTH_FOR_NOTIFICATION_TITLE
        ? Strings.safeSubstring(fullTitle, MAX_LENGTH_FOR_NOTIFICATION_TITLE) + "…"
        : fullTitle;
  }

  private void displayErrorNotification(MediaDownloadJob failedDownloadJob, int notificationId) {
    Intent retryIntent = MediaNotifActionReceiver.createRetryDownloadIntent(this, failedDownloadJob);
    PendingIntent retryPendingIntent = PendingIntent.getBroadcast(this,
        createRequestId(REQUESTCODE_RETRY_DOWNLOAD_PREFIX_, notificationId),
        retryIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    );

    Notification errorNotification = new NotificationCompat.Builder(this)
        .setContentTitle(getString(
            failedDownloadJob.mediaLink().isVideo()
                ? R.string.mediadownloadnotification_failed_to_save_video
                : R.string.mediadownloadnotification_failed_to_save_image
        ))
        .setContentText(getString(R.string.mediadownloadnotification_tap_to_retry_url, failedDownloadJob.mediaLink().originalUrl()))
        .setSmallIcon(R.drawable.ic_error_24dp)
        .setOngoing(false)
        .setGroup(NotificationConstants.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
        .setLocalOnly(true)   // Hide from wearables.
        .setWhen(0) // TODO: set this.
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        .setContentIntent(retryPendingIntent)
        .setAutoCancel(false)
        .build();
    NotificationManagerCompat.from(this).notify(notificationId, errorNotification);
  }

  private void displaySuccessNotification(MediaDownloadJob completedDownloadJob, int notificationId) {
    Timber.i("Displaying success notif for: %s", completedDownloadJob.mediaLink().originalUrl());

    // Content intent.
    Uri imageContentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), completedDownloadJob.downloadedFile());
    PendingIntent viewImagePendingIntent = PendingIntent.getActivity(this,
        createRequestId(REQUESTCODE_OPEN_IMAGE_PREFIX_, notificationId),
        Intents.createForViewingImage(this, imageContentUri),
        PendingIntent.FLAG_CANCEL_CURRENT
    );

    // Share action.
    PendingIntent shareImagePendingIntent = PendingIntent.getBroadcast(this,
        createRequestId(REQUESTCODE_SHARE_IMAGE_PREFIX_, notificationId),
        MediaNotifActionReceiver.createShareImageIntent(this, completedDownloadJob),
        PendingIntent.FLAG_CANCEL_CURRENT
    );
    NotificationCompat.Action shareImageAction = new NotificationCompat.Action(0,
        getString(R.string.mediadownloadnotification_share),
        shareImagePendingIntent
    );

    // Delete action.
    PendingIntent deleteImagePendingIntent = PendingIntent.getBroadcast(this,
        createRequestId(REQUESTCODE_DELETE_IMAGE_PREFIX_, notificationId),
        MediaNotifActionReceiver.createDeleteImageIntent(this, completedDownloadJob),
        PendingIntent.FLAG_CANCEL_CURRENT
    );
    NotificationCompat.Action deleteImageAction = new NotificationCompat.Action(0,
        getString(R.string.mediadownloadnotification_delete),
        deleteImagePendingIntent
    );

    Glide.with(this)
        .asBitmap()
        .load(Uri.fromFile(completedDownloadJob.downloadedFile()))
        .into(new SimpleTarget<Bitmap>() {
          @Override
          public void onResourceReady(Bitmap imageBitmap, Transition<? super Bitmap> transition) {
            Notification successNotification = new NotificationCompat.Builder(MediaDownloadService.this)
                .setContentTitle(getString(
                    completedDownloadJob.mediaLink().isVideo()
                        ? R.string.mediadownloadnotification_sucesss_title_for_video
                        : R.string.mediadownloadnotification_success_title_for_image
                ))
                .setContentText(getString(R.string.mediadownloadnotification_success_body))
                .setSmallIcon(R.drawable.ic_done_24dp)
                .setOngoing(false)
                .setGroup(NotificationConstants.MEDIA_DOWNLOAD_BUNDLE_NOTIFS_GROUP_KEY)
                .setLocalOnly(true)
                .setWhen(completedDownloadJob.timestamp())
                .setColor(ContextCompat.getColor(MediaDownloadService.this, R.color.notification_icon_color))
                .setContentIntent(viewImagePendingIntent)
                .addAction(shareImageAction)
                .addAction(deleteImageAction)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .setSummaryText(completedDownloadJob.mediaLink().originalUrl())
                )
                .build();

            Timber.i("Displaying success notif");
            NotificationManagerCompat.from(MediaDownloadService.this).notify(notificationId, successNotification);
          }
        });
  }

  // TODO: Remove random url.
  private Observable<MediaDownloadJob> downloadImage(MediaLink mediaLink) {
    String imageUrl = mediaLink.originalUrl()
        + "?" + String.valueOf(System.currentTimeMillis());
    long downloadStartTimeMillis = System.currentTimeMillis();

    return Observable.create(emitter -> {
      Target<File> fileFutureTarget = new SimpleTarget<File>() {
        @Override
        public void onResourceReady(File downloadedFile, Transition<? super File> transition) {
          long downloadCompleteTimeMillis = System.currentTimeMillis();
          emitter.onNext(MediaDownloadJob.createProgress(mediaLink, 100, downloadStartTimeMillis));
          emitter.onNext(MediaDownloadJob.createDownloaded(mediaLink, downloadedFile, downloadCompleteTimeMillis));
          emitter.onComplete();
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
          long downloadFailTimeMillis = System.currentTimeMillis();
          emitter.onNext(MediaDownloadJob.createFailed(mediaLink, downloadFailTimeMillis));
          emitter.onComplete();
        }
      };

      GlideProgressTarget<String, File> progressTarget = new GlideProgressTarget<String, File>(fileFutureTarget) {
        @Override
        public float getGranularityPercentage() {
          return 2f;
        }

        @Override
        protected void onConnecting() {
          emitter.onNext(MediaDownloadJob.createConnecting(mediaLink, downloadStartTimeMillis));
        }

        @Override
        protected void onDownloading(long bytesRead, long expectedLength) {
          int progress = (int) (100 * (float) bytesRead / expectedLength);
          Timber.i("%s: %s", mediaLink.originalUrl(), progress);
          emitter.onNext(MediaDownloadJob.createProgress(mediaLink, progress, downloadStartTimeMillis));
        }

        @Override
        protected void onDownloaded() {}

        @Override
        protected void onDelivered() {}
      };
      progressTarget.setModel(this, imageUrl);

      Glide.with(this).download(imageUrl).into(progressTarget);
      emitter.setCancellable(() -> Glide.with(this).clear(progressTarget));
    });
  }

  public static int createNotificationIdFor(MediaLink mediaLink) {
    return (NotificationConstants.MEDIA_DOWNLOAD_PROGRESS_PREFIX_ + mediaLink.originalUrl()).hashCode();
  }

  public static int createRequestId(String idPrefix, int idSuffix) {
    boolean isNegative = idSuffix < 0;
    long requestId = Long.parseLong(idPrefix + Math.abs(idSuffix));
    return (int) requestId * (isNegative ? -1 : 1);
  }
}
