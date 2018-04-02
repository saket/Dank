package me.saket.dank.di;

import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.cache.CacheModule;
import me.saket.dank.cache.DatabaseCacheRecyclerJobService;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.deeplinks.DeepLinkHandlingActivity;
import me.saket.dank.ui.appshortcuts.AppShortcutRepository;
import me.saket.dank.ui.appshortcuts.ConfigureAppShortcutsActivity;
import me.saket.dank.ui.subreddit.NewSubredditSubscriptionDialog;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.data.VotingManager;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.notifs.MediaDownloadService;
import me.saket.dank.notifs.MessageNotifActionReceiver;
import me.saket.dank.notifs.MessageNotifActionsJobService;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.ui.PlaygroundActivity;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.UploadImageDialog;
import me.saket.dank.ui.giphy.GiphyPickerActivity;
import me.saket.dank.ui.media.BaseMediaViewerFragment;
import me.saket.dank.ui.media.MediaAlbumViewerActivity;
import me.saket.dank.ui.media.MediaImageFragment;
import me.saket.dank.ui.media.MediaVideoFragment;
import me.saket.dank.ui.preferences.HiddenPreferencesActivity;
import me.saket.dank.ui.preferences.PreferenceGroupsScreen;
import me.saket.dank.ui.submission.CommentOptionsPopup;
import me.saket.dank.ui.submission.LinkOptionsPopup;
import me.saket.dank.ui.submission.RetryReplyJobService;
import me.saket.dank.ui.submission.SubmissionPageLayout;
import me.saket.dank.ui.submission.VoteJobService;
import me.saket.dank.ui.subreddit.SubmissionOptionsPopup;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.ui.subscriptions.SubredditPickerSheetView;
import me.saket.dank.ui.subscriptions.SubredditSubscriptionsSyncJob;
import me.saket.dank.ui.subreddit.UserProfileSheetView;
import me.saket.dank.ui.user.UserAuthListener;
import me.saket.dank.ui.user.UserProfilePopup;
import me.saket.dank.ui.user.messages.InboxActivity;
import me.saket.dank.ui.user.messages.InboxFolderFragment;
import me.saket.dank.ui.user.messages.PrivateMessageThreadActivity;
import me.saket.dank.utils.JacksonHelper;

@Component(modules = { RootModule.class, UserPreferencesModule.class, CacheModule.class })
@Singleton
public interface RootComponent {

  DankRedditClient dankRedditClient();

  DankApi api();

  SubscriptionRepository subredditSubscriptionRepository();

  JacksonHelper jacksonHelper();

  ErrorResolver errorManager();

  MessagesNotificationManager messagesNotificationManager();

  Moshi moshi();

  VotingManager votingManager();

  UserAuthListener userAuthListener();

  AppShortcutRepository shortcutRepository();

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

  void inject(VoteJobService target);

  void inject(SubmissionOptionsPopup target);

  void inject(CommentOptionsPopup target);

  void inject(LinkOptionsPopup target);

  void inject(PlaygroundActivity target);

  void inject(MessageNotifActionReceiver target);

  void inject(PreferenceGroupsScreen target);

  void inject(UserProfileSheetView target);

  void inject(ConfigureAppShortcutsActivity target);

  void inject(DeepLinkHandlingActivity target);

  void inject(NewSubredditSubscriptionDialog target);
}
