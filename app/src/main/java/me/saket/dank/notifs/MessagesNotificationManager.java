package me.saket.dank.notifs;

import static java.util.Collections.unmodifiableSet;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.CheckResult;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.MessagingStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;

public class MessagesNotificationManager {

  private static final int NOTIF_ID_BUNDLE_SUMMARY = 100;
  private static final String BUNDLED_NOTIFS_KEY = "unreadMessages";

  private final SeenUnreadMessagesIdStore seenMessageIdsStore;

  public MessagesNotificationManager(SharedPreferences sharedPreferences) {
    seenMessageIdsStore = new SeenUnreadMessagesIdStore(sharedPreferences);
  }

  /**
   * Recycle "seen" status of messages that are no longer present in user's unread inbox folder.
   * Everything else is retained.
   * <p>
   * A "seen" notification is one that the user has already dismissed.
   */
  @CheckResult
  public Completable updateSeenStatusOfUnreadMessagesWith(List<Message> newUnreadMessages) {
    return seenMessageIdsStore.get()
        .map(existingSeenMessageIds -> {
          Set<String> newStatusSet = new HashSet<>();

          // We'll only retain seen status of messages that are present in this
          // new list of unread messages. Everything else gets recycled.
          for (Message unreadMessage : newUnreadMessages) {
            String messageId = unreadMessage.getId();
            if (existingSeenMessageIds.contains(messageId)) {
              newStatusSet.add(messageId);
            }
          }
          return unmodifiableSet(newStatusSet);
        })
        .flatMapCompletable(seenMessageIds -> seenMessageIdsStore.save(seenMessageIds));
  }

  /**
   * Remove messages whose notifications the user has already seen (by dismissing it).
   */
  @CheckResult
  public Single<List<Message>> filterUnseenMessages(List<Message> unfilteredMessages) {
    return seenMessageIdsStore.get()
        .map(seenMessageIds -> {
          List<Message> unseenMessages = new ArrayList<>(unfilteredMessages.size());

          for (Message unfilteredMessage : unfilteredMessages) {
            if (!seenMessageIds.contains(unfilteredMessage.getId())) {
              unseenMessages.add(unfilteredMessage);
            }
          }

          return Collections.unmodifiableList(unseenMessages);
        });
  }

  @CheckResult
  public Completable markMessageNotificationsAsSeen(String... messageIds) {
    return seenMessageIdsStore.get()
        .flatMapCompletable(seenMessages -> {
          Set<String> updatedSeenMessages = new HashSet<>(seenMessages.size() + messageIds.length);
          Collections.addAll(updatedSeenMessages, messageIds);
          return seenMessageIdsStore.save(updatedSeenMessages);
        });
  }

  @CheckResult
  public Completable markMessageNotificationAsSeen(Message message) {
    return markMessageNotificationsAsSeen(message.getId());
  }

  private static class SeenUnreadMessagesIdStore {
    private final SharedPreferences sharedPreferences;

    private static final String KEY_SEEN_UNREAD_MESSAGES = "seenUnreadMessages";

    public SeenUnreadMessagesIdStore(SharedPreferences sharedPreferences) {
      this.sharedPreferences = sharedPreferences;
    }

    /**
     * @param seenMessageIds IDs of unread messages whose notifications the user has already seen.
     */
    @CheckResult
    private Completable save(Set<String> seenMessageIds) {
      return Completable.fromAction(() -> sharedPreferences.edit().putStringSet(KEY_SEEN_UNREAD_MESSAGES, seenMessageIds).apply());
    }

    /**
     * @return Message IDs that the user has already seen.
     */
    @CheckResult
    private Single<Set<String>> get() {
      return Single.fromCallable(() -> {
        Set<String> seenMessageIdSet = sharedPreferences.getStringSet(KEY_SEEN_UNREAD_MESSAGES, Collections.emptySet());
        return unmodifiableSet(seenMessageIdSet);
      });
    }
  }

// ======== NOTIFICATION ======== //

  public Completable displayNotification(Context context, List<Message> unreadMessages) {
    return Completable.fromAction(() -> {
      String loggedInUserName = Dank.reddit().loggedInUserName();
      createNotification(context, loggedInUserName, unreadMessages);
    });
  }

  private void createNotification(Context context, String loggedInUserName, List<Message> unreadMessages) {
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

    // We'll create a "MessagingStyle" summary notification for the bundled notifs.
    MessagingStyle messagingStyleBuilder = new MessagingStyle(loggedInUserName);
    messagingStyleBuilder.setConversationTitle(unreadMessages.size() + " new messages");

    // Markdown requires a TextPaint object for constructing
    // ordered points. Sending an empty object also works for us.
    TextPaint textPaint = new TextPaint();

    for (Message unreadMessage : unreadMessages) {
      long timestamp = JrawUtils.createdTimeUtc(unreadMessage);
      Markdown.parseRedditMarkdownHtml(JrawUtils.getMessageBodyHtml(unreadMessage), textPaint);
      messagingStyleBuilder.addMessage(new MessagingStyle.Message(unreadMessage.getBody(), timestamp, unreadMessage.getAuthor()));
    }

    // Mark all as seen on summary notif dismissal.
    Intent markAllAsSeenIntent = NotificationActionReceiver.createMarkAllAsSeenIntent(context, unreadMessages);
    PendingIntent summaryDeletePendingIntent = PendingIntent.getBroadcast(
        context,
        NOTIF_ID_BUNDLE_SUMMARY,
        markAllAsSeenIntent,
        PendingIntent.FLAG_ONE_SHOT
    );

    String notifBody = unreadMessages.size() == 1 ? "From " + unreadMessages.get(0).getAuthor() : "From " + unreadMessages.size() + " users";

    Notification summaryNotification = new NotificationCompat.Builder(context)
        .setContentTitle(unreadMessages.size() + " new messages")
        .setContentText(notifBody)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setSubText(loggedInUserName)
        .setGroup(BUNDLED_NOTIFS_KEY)
        .setShowWhen(true)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .setDeleteIntent(summaryDeletePendingIntent)
        .setColor(ContextCompat.getColor(context, R.color.color_accent))
        .setStyle(messagingStyleBuilder)
        .build();
    notificationManager.notify(NOTIF_ID_BUNDLE_SUMMARY, summaryNotification);

    for (Message unreadMessage : unreadMessages) {
      int notificationId = (int) JrawUtils.createdTimeUtc(unreadMessage);

      // Mark as read action.
      Intent markAsReadIntent = NotificationActionReceiver.createMarkAsReadIntent(context, unreadMessage, Dank.jackson());
      PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(context, notificationId, markAsReadIntent, PendingIntent.FLAG_ONE_SHOT);
      NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(0, "Mark read", markAsReadPendingIntent).build();

      // Direct reply action.
      Intent directReplyIntent = NotificationActionReceiver.createDirectReplyIntent(context, unreadMessage, notificationId, Dank.jackson());
      PendingIntent directReplyPendingIntent = PendingIntent.getBroadcast(context, notificationId, directReplyIntent, PendingIntent.FLAG_ONE_SHOT);
      RemoteInput directReplyInput = new RemoteInput.Builder(NotificationActionReceiver.KEY_DIRECT_REPLY_MESSAGE)
          .setLabel(context.getString(R.string.messagenotification_quick_reply))
          .build();
      NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(0, context.getString(R.string.messagenotification_reply), directReplyPendingIntent)
          .addRemoteInput(directReplyInput)
          .setAllowGeneratedReplies(true)
          .build();

      // Dismissal intent.
      Intent markAsSeenIntent = NotificationActionReceiver.createMarkAsSeenIntent(context, unreadMessage);
      PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, notificationId, markAsSeenIntent, PendingIntent.FLAG_ONE_SHOT);

      Notification bundledNotification = new NotificationCompat.Builder(context)
          .setContentTitle(unreadMessage.getAuthor())
          .setContentText(unreadMessage.getBody())
          .setShowWhen(false)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setGroup(BUNDLED_NOTIFS_KEY)
          .setAutoCancel(true)
          .setColor(ContextCompat.getColor(context, R.color.color_accent))
          .addAction(replyAction)
          .addAction(markAsReadAction)
          .setDeleteIntent(deletePendingIntent)
          .build();
      notificationManager.notify(notificationId, bundledNotification);
    }
  }

  public Completable dismissNotification(Context context, int notificationId) {
    return Completable.fromAction(() -> {
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.cancel(notificationId);
    });
  }

  /**
   * Dismiss the summary notification of a bundle so that everything gets dismissed.
   */
  public Completable dismissAllNotifications(Context context) {
      return Completable.fromAction(() -> {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIF_ID_BUNDLE_SUMMARY);
      });
  }

}
