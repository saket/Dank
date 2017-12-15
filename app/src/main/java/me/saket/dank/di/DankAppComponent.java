package me.saket.dank.di;

import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.DatabaseCacheRecyclerJobService;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.notifs.MediaDownloadService;
import me.saket.dank.notifs.MessageNotifActionsJobService;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.UploadImageDialog;
import me.saket.dank.ui.giphy.GiphyPickerActivity;
import me.saket.dank.ui.media.BaseMediaViewerFragment;
import me.saket.dank.ui.media.MediaAlbumViewerActivity;
import me.saket.dank.ui.media.MediaImageFragment;
import me.saket.dank.ui.media.MediaVideoFragment;
import me.saket.dank.ui.preferences.HiddenPreferencesActivity;
import me.saket.dank.ui.submission.RetryReplyJobService;
import me.saket.dank.ui.submission.SubmissionPageLayout;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.ui.subreddits.SubredditPickerSheetView;
import me.saket.dank.ui.subreddits.SubredditSubscriptionsSyncJob;
import me.saket.dank.ui.user.UserAuthListener;
import me.saket.dank.ui.user.UserProfilePopup;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.ui.user.messages.InboxActivity;
import me.saket.dank.ui.user.messages.InboxFolderFragment;
import me.saket.dank.ui.user.messages.PrivateMessageThreadActivity;
import me.saket.dank.utils.JacksonHelper;

@Component(modules = DankAppModule.class)
@Singleton
public interface DankAppComponent {

  DankRedditClient dankRedditClient();

  DankApi api();

  SubredditSubscriptionManager subredditSubscriptionManager();

  JacksonHelper jacksonHelper();

  ErrorResolver errorManager();

  MessagesNotificationManager messagesNotificationManager();

  Moshi moshi();

  VotingManager votingManager();

  UserSessionRepository userSession();

  UserAuthListener userAuthListener();

  void inject(MediaAlbumViewerActivity target);

  void inject(MediaVideoFragment target);

  void inject(SubmissionPageLayout target);

  void inject(MediaDownloadService target);

  void inject(MediaImageFragment target);

  void inject(InboxActivity target);

  void inject(PrivateMessageThreadActivity target);

  void inject(UserProfilePopup target);

  void inject(HiddenPreferencesActivity target);

  void inject(SubredditActivity target);

  void inject(SubredditSubscriptionsSyncJob target);

  void inject(SubredditPickerSheetView target);

  void inject(DatabaseCacheRecyclerJobService target);

  void inject(UploadImageDialog target);

  void inject(GiphyPickerActivity target);

  void inject(RetryReplyJobService target);

  void inject(ComposeReplyActivity target);

  void inject(InboxFolderFragment target);

  void inject(BaseMediaViewerFragment target);

  void inject(CheckUnreadMessagesJobService target);

  void inject(MessageNotifActionsJobService target);

  void inject(LoginActivity target);
}
