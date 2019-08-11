package me.saket.dank.notifs;

import static me.saket.dank.ui.media.MediaDownloadJob.ProgressState.DOWNLOADED;
import static me.saket.dank.ui.media.MediaDownloadJob.ProgressState.FAILED;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.media.session.MediaSessionCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.danikula.videocache.HttpProxyCacheServer;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.media.MediaDownloadJob;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.media.MediaLinkWithStartingPosition;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditHostedVideoLink;
import me.saket.dank.utils.Files2;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.VideoFormat;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.utils.okhttp.OkHttpResponseBodyWithProgress;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

/**
 * Downloads images and videos to disk.
 * FIXME: Move all logic to a separate testable class.
 * FIXME: Use a single source of truth for notifications.
 */
public class MediaDownloadService extends Service {

  private static final String KEY_MEDIA_LINK_TO_DOWNLOAD = "mediaLinkToDownload";
  private static final String KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD = "mediaLinkToCancelDownload";
  private static final String KEY_MEDIA_LINK_TO_CANCEL_NOTIF = "mediaLinkToDismissNotif";
  private static final String KEY_ACTION = "action";
  private static final int MAX_LENGTH_FOR_NOTIFICATION_TITLE = 40;
  private static final String REQUESTCODE_RETRY_DOWNLOAD_PREFIX_ = "300";
  private static final String REQUESTCODE_SHARE_IMAGE_PREFIX_ = "301";
  private static final String REQUESTCODE_DELETE_IMAGE_PREFIX_ = "302";
  private static final String REQUESTCODE_OPEN_IMAGE_PREFIX_ = "303";
  private static final String REQUESTCODE_CANCEL_DOWNLOAD_PREFIX_ = "304";

  /**
   * Starting from Nougat, Android has a rate limiter in place which puts a certain
   * threshold between every update. The exact value is somewhere between 100ms to 200ms.
   */
  private static final int MINIMUM_GAP_BETWEEN_NOTIFICATION_UPDATEs = 201;

  @Inject HttpProxyCacheServer videoCacheServer;
  @Inject OkHttpClient okHttpClient;
  @Inject MediaHostRepository mediaHostRepository;

  private CompositeDisposable disposables = new CompositeDisposable();
  private final Set<MediaLink> ongoingDownloadLinks = new HashSet<>();
  private final Map<MediaLink, MediaDownloadJob> downloadJobsWithVisibleNotif = new HashMap<>();
  private final Relay<MediaLink> downloadRequestStream = PublishRelay.create();
  private final Relay<MediaLink> downloadCancellationStream = PublishRelay.create();

  enum Action {
    ENQUEUE_DOWNLOAD,
    CANCEL_DOWNLOAD,
    CANCEL_NOTIFICATION,  // Canceled programmatically.
  }

  public static void enqueueDownload(Context context, MediaLink mediaLink) {
    if (mediaLink instanceof MediaLinkWithStartingPosition) {
      mediaLink = ((MediaLinkWithStartingPosition) mediaLink).delegate();
    }

    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_DOWNLOAD, mediaLink);
    intent.putExtra(KEY_ACTION, Action.ENQUEUE_DOWNLOAD);
    context.startService(intent);
  }

  private static Intent cancelDownloadIntent(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD, mediaLink);
    intent.putExtra(KEY_ACTION, Action.CANCEL_DOWNLOAD);
    return intent;
  }

  /**
   * Called when a notification is canceled internally by the app.
   */
  static void cancelNotification(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaDownloadService.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_CANCEL_NOTIF, mediaLink);
    intent.putExtra(KEY_ACTION, Action.CANCEL_NOTIFICATION);
    context.startService(intent);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();

    // Stop service when all downloads finish.
    Relay<Collection<MediaDownloadJob>> activeDownloadsProgressChangeStream = PublishRelay.create();
    disposables.add(
        activeDownloadsProgressChangeStream
            .map(activeDownloadJobs -> {
              boolean allDownloadsTerminated = true;
              for (MediaDownloadJob downloadJob : activeDownloadJobs) {
                if (downloadJob.progressState() != DOWNLOADED && downloadJob.progressState() != FAILED) {
                  allDownloadsTerminated = false;
                }
              }
              return allDownloadsTerminated;
            })
            .filter(allDownloadsComplete -> allDownloadsComplete)
            .subscribe(o -> stopSelf())
    );

    disposables.add(
        downloadCancellationStream.subscribe(mediaLinkToCancel -> {
          ongoingDownloadLinks.remove(mediaLinkToCancel);
          NotificationManagerCompat.from(this).cancel(createNotificationIdFor(mediaLinkToCancel));
        })
    );

    disposables.add(
        downloadRequestStream
            .sample(MINIMUM_GAP_BETWEEN_NOTIFICATION_UPDATEs, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .doOnNext(link -> Timber.i("Recvd request for %s", link))
            .doOnNext(linkToQueue -> {
              Timber.i("Showing queued notif");
              MediaDownloadJob downloadJobToQueue = MediaDownloadJob.queued(linkToQueue, System.currentTimeMillis());
              downloadJobsWithVisibleNotif.put(downloadJobToQueue.mediaLink(), downloadJobToQueue);
              updateIndividualProgressNotification(downloadJobToQueue, createNotificationIdFor(linkToQueue));
            })
            .concatMap(linkToDownload -> {
              Timber.i("Downloading %s", linkToDownload);
              Observable<MediaDownloadJob> downloadStream;
              if (linkToDownload.isVideo()) {
                downloadStream = downloadVideoAndStreamProgress(linkToDownload).compose(RxUtils.applySchedulers());
              } else {
                downloadStream = downloadImageAndStreamProgress(linkToDownload);
              }

              return downloadStream
                  .unsubscribeOn(Schedulers.io())
                  .map(moveFileToUserSpaceOnDownload())
                  .doOnTerminate(() -> ongoingDownloadLinks.remove(linkToDownload))
                  .doOnError(e -> Timber.e(e, "Couldn't download media"))
                  .onErrorReturnItem(MediaDownloadJob.failed(linkToDownload, System.currentTimeMillis()))
                  .takeUntil(downloadCancellationStream.filter(linkToCancel -> linkToCancel.equals(linkToDownload)))
                  .sample(MINIMUM_GAP_BETWEEN_NOTIFICATION_UPDATEs, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread(), true);
            })
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
                  ongoingDownloadLinks.remove(downloadJob.mediaLink());
                  break;

                case DOWNLOADED:
                  displaySuccessNotification(downloadJob, notificationId);
                  ongoingDownloadLinks.remove(downloadJob.mediaLink());
                  break;

                default:
                  throw new UnsupportedOperationException("Unknown state: " + downloadJob.progressState());
              }

              downloadJobsWithVisibleNotif.put(downloadJob.mediaLink(), downloadJob);
              activeDownloadsProgressChangeStream.accept(downloadJobsWithVisibleNotif.values());
            })
    );
  }

  @Override
  public void onDestroy() {
    Timber.i("Stopping service");
    disposables.clear();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Action serviceAction = (Action) intent.getSerializableExtra(KEY_ACTION);
    switch (serviceAction) {
      case ENQUEUE_DOWNLOAD:
        MediaLink mediaLinkToDownload = intent.getParcelableExtra(KEY_MEDIA_LINK_TO_DOWNLOAD);
        boolean isDownloadAlreadyOngoing = ongoingDownloadLinks.contains(mediaLinkToDownload);
        if (!isDownloadAlreadyOngoing) {
          Timber.i("Enqueuing %s", mediaLinkToDownload);
          ongoingDownloadLinks.add(mediaLinkToDownload);
          downloadRequestStream.accept(mediaLinkToDownload);
        } else {
          Timber.w("Ignoring ongoing download");
        }
        break;

      case CANCEL_DOWNLOAD:
        MediaLink mediaLinkToCancel = intent.getParcelableExtra(KEY_MEDIA_LINK_TO_CANCEL_DOWNLOAD);
        downloadCancellationStream.accept(mediaLinkToCancel);
        break;

      case CANCEL_NOTIFICATION:
        MediaLink mediaLinkToCancelNotif = intent.getParcelableExtra(KEY_MEDIA_LINK_TO_CANCEL_NOTIF);
        downloadCancellationStream.accept(mediaLinkToCancelNotif);
        break;

      default:
        throw new UnsupportedOperationException("Unknown action: " + serviceAction);
    }
    return START_NOT_STICKY;
  }

  private void updateIndividualProgressNotification(MediaDownloadJob mediaDownloadJob, int notificationId) {
    boolean isQueued = mediaDownloadJob.progressState() == MediaDownloadJob.ProgressState.QUEUED;
    String notificationTitle = ellipsizeNotifTitleIfExceedsMaxLength(getString(
        isQueued ? R.string.mediadownloadnotification_queued_title : R.string.mediadownloadnotification_progress_title,
        mediaDownloadJob.mediaLink().unparsedUrl()
    ));
    boolean indeterminateProgress = mediaDownloadJob.progressState() == MediaDownloadJob.ProgressState.CONNECTING;

    NotificationCompat.Action cancelAction = new NotificationCompat.Action(0,
        getString(R.string.mediadownloadnotification_cancel),
        PendingIntent.getService(this,
            createPendingIntentRequestId(REQUESTCODE_CANCEL_DOWNLOAD_PREFIX_, notificationId),
            cancelDownloadIntent(this, mediaDownloadJob.mediaLink()),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    );

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_media_downloads_id))
        .setContentTitle(notificationTitle)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .setLocalOnly(true)   // Hide from wearables.
        .setGroup(NotificationConstants.MEDIA_DOWNLOAD_GROUP)
        .setWhen(mediaDownloadJob.timestamp())
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        // Keep notification of ongoing-download above queued-downloads.
        .setPriority(isQueued ? Notification.PRIORITY_LOW : Notification.PRIORITY_DEFAULT)
        .setProgress(100 /* max */, mediaDownloadJob.downloadProgress(), indeterminateProgress)
        .setOnlyAlertOnce(true)
        .addAction(cancelAction);

    if (mediaDownloadJob.progressState() != MediaDownloadJob.ProgressState.CONNECTING) {
      notificationBuilder = notificationBuilder.setContentText(mediaDownloadJob.downloadProgress() + "%");
    }

    Notification notification = notificationBuilder.build();
    NotificationManagerCompat.from(this).notify(notificationId, notification);
  }

  /**
   * We're ellipsizing the title so that the notification's content text (which is the progress
   * percentage at the time of writing this) is always visible.
   */
  private static String ellipsizeNotifTitleIfExceedsMaxLength(String fullTitle) {
    return fullTitle.length() > MAX_LENGTH_FOR_NOTIFICATION_TITLE
        ? Strings.substringWithBounds(fullTitle, MAX_LENGTH_FOR_NOTIFICATION_TITLE) + "…"
        : fullTitle;
  }

  private void displayErrorNotification(MediaDownloadJob failedDownloadJob, int notificationId) {
    Intent retryIntent = MediaNotifActionReceiver.createRetryDownloadIntent(this, failedDownloadJob);
    PendingIntent retryPendingIntent = PendingIntent.getBroadcast(this,
        createPendingIntentRequestId(REQUESTCODE_RETRY_DOWNLOAD_PREFIX_, notificationId),
        retryIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    );

    Notification errorNotification = new NotificationCompat.Builder(this, getString(R.string.notification_channel_media_downloads_id))
        .setContentTitle(getString(
            failedDownloadJob.mediaLink().isVideo()
                ? R.string.mediadownloadnotification_failed_to_save_video
                : R.string.mediadownloadnotification_failed_to_save_image
        ))
        .setContentText(getString(R.string.mediadownloadnotification_tap_to_retry_url, failedDownloadJob.mediaLink().unparsedUrl()))
        .setSmallIcon(R.drawable.ic_error_24dp)
        .setOngoing(false)
        .setLocalOnly(true)   // Hide from wearables.
        .setGroup(NotificationConstants.MEDIA_DOWNLOAD_GROUP)
        .setWhen(failedDownloadJob.timestamp())
        .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
        .setContentIntent(retryPendingIntent)
        .setAutoCancel(false)
        .build();
    NotificationManagerCompat.from(this).notify(notificationId, errorNotification);
  }

  /**
   * Generate a notification with a preview of the media. Images and videos both work, thanks to Glide.
   */
  private void displaySuccessNotification(MediaDownloadJob completedDownloadJob, int notificationId) {
    // Content intent.
    Uri mediaContentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), completedDownloadJob.downloadedFile());

    PendingIntent viewImagePendingIntent = PendingIntent.getActivity(this,
        createPendingIntentRequestId(REQUESTCODE_OPEN_IMAGE_PREFIX_, notificationId),
        Intents.createForViewingMedia(this, mediaContentUri, completedDownloadJob.mediaLink().isVideo()),
        PendingIntent.FLAG_CANCEL_CURRENT
    );

    // Share action.
    PendingIntent shareImagePendingIntent = PendingIntent.getBroadcast(this,
        createPendingIntentRequestId(REQUESTCODE_SHARE_IMAGE_PREFIX_, notificationId),
        MediaNotifActionReceiver.createShareImageIntent(this, completedDownloadJob),
        PendingIntent.FLAG_CANCEL_CURRENT
    );
    NotificationCompat.Action shareImageAction = new NotificationCompat.Action(
        R.drawable.ic_share_20dp,
        getString(R.string.mediadownloadnotification_share),
        shareImagePendingIntent
    );

    // Delete action.
    PendingIntent deleteImagePendingIntent = PendingIntent.getBroadcast(this,
        createPendingIntentRequestId(REQUESTCODE_DELETE_IMAGE_PREFIX_, notificationId),
        MediaNotifActionReceiver.createDeleteImageIntent(this, completedDownloadJob),
        PendingIntent.FLAG_CANCEL_CURRENT
    );
    NotificationCompat.Action deleteImageAction = new NotificationCompat.Action(
        R.drawable.ic_delete_20dp,
        getString(R.string.mediadownloadnotification_delete),
        deleteImagePendingIntent
    );

    Glide.with(this)
        .asBitmap()
        .load(Uri.fromFile(completedDownloadJob.downloadedFile()))
        .into(new SimpleTarget<Bitmap>() {
          @Override
          public void onResourceReady(Bitmap imageBitmap, Transition<? super Bitmap> transition) {
            String notificationChannelId = getString(R.string.notification_channel_media_downloads_id);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MediaDownloadService.this, notificationChannelId)
                .setContentTitle(getString(
                    completedDownloadJob.mediaLink().isVideo()
                        ? R.string.mediadownloadnotification_sucesss_title_for_video
                        : R.string.mediadownloadnotification_success_title_for_image
                ))
                .setSmallIcon(R.drawable.ic_done_24dp)
                .setOngoing(false)
                .setLocalOnly(true)
                .setGroup(NotificationConstants.MEDIA_DOWNLOAD_GROUP)
                .setWhen(completedDownloadJob.timestamp())
                .setContentIntent(viewImagePendingIntent)
                .addAction(shareImageAction)
                .addAction(deleteImageAction)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_ALL);

            // Taking advantage of O's tinted media notifications! I feel bad for this.
            // Let's see if anyone from Google asks me to remove this.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              MediaSession poop = new MediaSession(getBaseContext(), "me.saket.Dank.dummyMediaSession");
              MediaSessionCompat.Token dummyTokenCompat = MediaSessionCompat.Token.fromToken(poop.getSessionToken());
              poop.release();

              notificationBuilder = notificationBuilder
                  .setLargeIcon(imageBitmap)
                  .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                      .setMediaSession(dummyTokenCompat)
                      .setShowActionsInCompactView(0 /* index of share action */)
                  )
                  .setContentText(completedDownloadJob.mediaLink().unparsedUrl())
              ;

            } else {
              notificationBuilder = notificationBuilder
                  .setColor(ContextCompat.getColor(MediaDownloadService.this, R.color.notification_icon_color))
                  .setStyle(new NotificationCompat.BigPictureStyle()
                      .bigPicture(imageBitmap)
                      .setSummaryText(completedDownloadJob.mediaLink().unparsedUrl())
                  )
                  .setContentText(getString(R.string.mediadownloadnotification_success_body));
            }

            Notification successNotification = notificationBuilder.build();
            NotificationManagerCompat.from(MediaDownloadService.this).notify(notificationId, successNotification);
          }
        });
  }

  /**
   * Download an image and streams progress updates.
   */
  private Observable<MediaDownloadJob> downloadImageAndStreamProgress(MediaLink mediaLink) {
    long downloadStartTimeMillis = System.currentTimeMillis();

    return Observable.create(emitter -> {
      Target<File> fileTarget = new SimpleTarget<File>() {
        @Override
        public void onResourceReady(File downloadedFile, @Nullable Transition<? super File> transition) {
          long downloadCompleteTimeMillis = System.currentTimeMillis();
          emitter.onNext(MediaDownloadJob.downloaded(mediaLink, downloadedFile, downloadCompleteTimeMillis));
          emitter.onComplete();
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
          long downloadFailTimeMillis = System.currentTimeMillis();
          emitter.onNext(MediaDownloadJob.failed(mediaLink, downloadFailTimeMillis));
          emitter.onComplete();
        }
      };

      GlideProgressTarget<String, File> progressTarget = new GlideProgressTarget<String, File>(fileTarget) {
        @Override
        public float getGranularityPercentage() {
          return 1f;
        }

        @Override
        protected void onConnecting() {
          emitter.onNext(MediaDownloadJob.connecting(mediaLink, downloadStartTimeMillis));
        }

        @Override
        protected void onDownloading(long bytesRead, long expectedBytes) {
          int progress = (int) (100 * (float) bytesRead / expectedBytes);
          emitter.onNext(MediaDownloadJob.progress(mediaLink, progress, downloadStartTimeMillis));
        }

        @Override
        protected void onDownloaded() {}

        @Override
        protected void onDelivered() {}
      };

      String imageUrl = mediaLink.highQualityUrl();
      progressTarget.setModel(this, imageUrl);
      Glide.with(this).download(imageUrl).into(progressTarget);

      emitter.setCancellable(() -> Glide.with(this).clear(progressTarget));
    });
  }

  /**
   * Download a video and streams progress updates.
   */
  private Observable<MediaDownloadJob> downloadVideoAndStreamProgress(MediaLink linkToDownload) {
    return Observable.create(emitter -> {
      long downloadStartTimeMillis = System.currentTimeMillis();

      String highQualityUrl = linkToDownload.highQualityUrl();
      VideoFormat videoFormat = VideoFormat.parse(highQualityUrl);

      if (videoFormat.canBeCached() && videoCacheServer.isCached(highQualityUrl)) {
        String cachedVideoFileUrl = videoCacheServer.getProxyUrl(highQualityUrl);
        File cachedVideoFile = new File(Uri.parse(cachedVideoFileUrl).getPath());
        emitter.onNext(MediaDownloadJob.downloaded(linkToDownload, cachedVideoFile, System.currentTimeMillis()));

      } else {
        String videoUrlToDownload;
        if (linkToDownload instanceof RedditHostedVideoLink) {
          videoUrlToDownload = ((RedditHostedVideoLink) linkToDownload).directUrlWithoutAudio();
        } else if (videoFormat.canBeCached()) {
          // Proxy through VideoCacheServer so that the downloaded video also gets saved to cache.
          videoUrlToDownload = videoCacheServer.getProxyUrl(highQualityUrl, false);
        } else {
          throw new UnsupportedOperationException("Couldn't figure out the video url for " + linkToDownload);
        }

        if (videoUrlToDownload == null) {
          emitter.onNext(MediaDownloadJob.failed(linkToDownload, System.currentTimeMillis()));
        } else {
          emitter.onNext(MediaDownloadJob.connecting(linkToDownload, downloadStartTimeMillis));

          // Emit progress updates while reading the video's input stream.
          Request downloadRequest = new Request.Builder()
              .url(videoUrlToDownload)
              .get()
              .build();
          Call networkCall = okHttpClient.newCall(downloadRequest);
          Response response = networkCall.execute();
          Response responseWithProgressListener = response.newBuilder()
              .body(OkHttpResponseBodyWithProgress.wrap(
                  downloadRequest,
                  response,
                  (url, bytesRead, expectedContentLength) -> {
                    if (bytesRead < expectedContentLength) {
                      int progress = (int) (100 * (float) bytesRead / expectedContentLength);
                      emitter.onNext(MediaDownloadJob.progress(linkToDownload, progress, downloadStartTimeMillis));
                    }
                  }
              ))
              .build();

          if (!responseWithProgressListener.isSuccessful()) {
            throw new IOException("Unexpected code: " + responseWithProgressListener);
          }

          // Write to a temporary file, that will later get replaced by moveFileToUserSpaceOnDownload().
          File videoTempFile = new File(getCacheDir(), Urls.parseFileNameWithExtension(highQualityUrl));
          //noinspection ConstantConditions
          try (BufferedSource bufferedSource = responseWithProgressListener.body().source()) {
            BufferedSink bufferedSink = Okio.buffer(Okio.sink(videoTempFile));
            bufferedSink.writeAll(bufferedSource);
            bufferedSink.close();
          }

          long downloadCompleteTimeMillis = System.currentTimeMillis();
          emitter.onNext(MediaDownloadJob.downloaded(linkToDownload, videoTempFile, downloadCompleteTimeMillis));
          emitter.onComplete();

          emitter.setCancellable(() -> {
            // Note: BufferedSink#writeAll() will also receive a thread interruption so file copy will stop.
            networkCall.cancel();
          });
        }
      }
    });
  }

  private Function<MediaDownloadJob, MediaDownloadJob> moveFileToUserSpaceOnDownload() {
    return downloadJobUpdate -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          throw new AssertionError("Storage permission not granted");
        }
      }

      if (downloadJobUpdate.progressState() == DOWNLOADED) {
        MediaLink downloadedMediaLink = downloadJobUpdate.mediaLink();
        String mediaFileName = Urls.parseFileNameWithExtension(downloadedMediaLink.highQualityUrl());
        //noinspection LambdaParameterTypeCanBeSpecified,ConstantConditions
        File userAccessibleFile = Files2.INSTANCE.copyFileToPicturesDirectory(getResources(), downloadJobUpdate.downloadedFile(), mediaFileName);

        //broadcast file
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, mediaFileName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, mediaFileName);
        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        return MediaDownloadJob.downloaded(downloadedMediaLink, userAccessibleFile, downloadJobUpdate.timestamp());

      } else {
        return downloadJobUpdate;
      }
    };
  }

  public static int createNotificationIdFor(MediaLink mediaLink) {
    return (NotificationConstants.ID_MEDIA_DOWNLOAD_PROGRESS_PREFIX_ + mediaLink.unparsedUrl()).hashCode();
  }

  public static int createPendingIntentRequestId(String idPrefix, int idSuffix) {
    boolean isNegative = idSuffix < 0;
    long requestId = Long.parseLong(idPrefix + Math.abs(idSuffix));
    return (int) requestId * (isNegative ? -1 : 1);
  }
}
