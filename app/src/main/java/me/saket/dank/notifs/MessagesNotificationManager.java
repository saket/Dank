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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import timber.log.Timber;

public class MessagesNotificationManager {

  private static final int NOTIF_ID_BUNDLE_SUMMARY = 100;
  private static final String BUNDLED_NOTIFS_KEY = "unreadMessages";

  private final SeenUnreadMessageIdStore seenMessageIdsStore;

  public MessagesNotificationManager(SeenUnreadMessageIdStore seenMessageIdsStore) {
    this.seenMessageIdsStore = seenMessageIdsStore;
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
            } else {
              Timber.w("Already seen: %s", unfilteredMessage.getBody());
            }
          }

          return Collections.unmodifiableList(unseenMessages);
        });
  }

  @CheckResult
  public Completable markMessageNotifAsSeen(String... messageIds) {
    return seenMessageIdsStore.get()
        .flatMapCompletable(seenMessageIds -> {
          Set<String> updatedSeenMessages = new HashSet<>(seenMessageIds.size() + messageIds.length);
          updatedSeenMessages.addAll(seenMessageIds);
          Collections.addAll(updatedSeenMessages, messageIds);
          return seenMessageIdsStore.save(updatedSeenMessages);
        });
  }

  @CheckResult
  public Completable markMessageNotifAsSeen(Message message) {
    return markMessageNotifAsSeen(message.getId());
  }

  /**
   * Recycle <var>message</var>'s ID when it's no longer unread.
   */
  @CheckResult
  public Completable removeMessageNotifSeenStatus(Message message) {
    return seenMessageIdsStore.get()
        .map(oldSeenMessageIds -> {
          Set<String> updatedSeenMessageIds = new HashSet<>(oldSeenMessageIds.size());
          updatedSeenMessageIds.addAll(oldSeenMessageIds);
          updatedSeenMessageIds.remove(message.getId());
          return Collections.unmodifiableSet(updatedSeenMessageIds);
        })
        .toCompletable();
  }

  /**
   * Recycle all "seen" message IDs.
   */
  @CheckResult
  public Completable removeAllMessageNotifSeenStatuses() {
    return seenMessageIdsStore.save(Collections.emptySet());
  }

  public static class SeenUnreadMessageIdStore {
    private final SharedPreferences sharedPreferences;

    private static final String KEY_SEEN_UNREAD_MESSAGES = "seenUnreadMessages";

    public SeenUnreadMessageIdStore(SharedPreferences sharedPreferences) {
      this.sharedPreferences = sharedPreferences;
    }

    /**
     * @param seenMessageIds IDs of unread messages whose notifications the user has already seen.
     */
    @CheckResult
    public Completable save(Set<String> seenMessageIds) {
      return Completable.fromAction(() -> sharedPreferences.edit().putStringSet(KEY_SEEN_UNREAD_MESSAGES, seenMessageIds).apply());
    }

    /**
     * @return Message IDs that the user has already seen.
     */
    @CheckResult
    public Single<Set<String>> get() {
      return Single.fromCallable(() -> {
        Set<String> seenMessageIdSet = sharedPreferences.getStringSet(KEY_SEEN_UNREAD_MESSAGES, Collections.emptySet());
        return unmodifiableSet(seenMessageIdSet);
      });
    }
  }

// ======== NOTIFICATION ======== //

  public Completable displayNotification(Context context, List<Message> unreadMessages) {
    return Completable.fromAction(() -> {
      Timber.i("Creating notif for:");
      String loggedInUserName = Dank.reddit().loggedInUserName();

      List<Message> timeSortedMessages = new ArrayList<>(unreadMessages.size());
      timeSortedMessages.addAll(unreadMessages);
      timeSortedMessages.sort((first, second) -> {
        Date firstDate = first.getCreated();
        Date secondDate = second.getCreated();

        if (firstDate.after(secondDate)) {
          return -1;  // First is older than second.
        } else if (secondDate.after(firstDate)) {
          return +1;  // Second is older than first.
        } else {
          return 0;   // Equal
        }
      });

      timeSortedMessages.forEach(m -> Timber.i("%s", m.getBody()));
      createNotification(context, loggedInUserName, Collections.unmodifiableList(timeSortedMessages));
    });
  }

  /**
   * Constructs bundled notifications for unread messages.
   */
  private void createNotification(Context context, String loggedInUserName, List<Message> sortedUnreadMessages) {
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

    // We'll create a "MessagingStyle" summary notification for the bundled notifs.
    MessagingStyle messagingStyleBuilder = new MessagingStyle(loggedInUserName);
    //messagingStyleBuilder.setConversationTitle(sortedUnreadMessages.size() + " new messages");

    // Markdown requires a TextPaint object for constructing ordered points. Sending
    // an empty object also works for us.
    TextPaint textPaint = new TextPaint();

    for (Message unreadMessage : sortedUnreadMessages) {
      long timestamp = JrawUtils.createdTimeUtc(unreadMessage);
      Markdown.parseRedditMarkdownHtml(JrawUtils.getMessageBodyHtml(unreadMessage), textPaint);
      messagingStyleBuilder.addMessage(new MessagingStyle.Message(unreadMessage.getBody(), timestamp, unreadMessage.getAuthor()));
    }

    // Mark all as seen on summary notif dismissal.
    Intent markAllAsSeenIntent = NotificationActionReceiver.createMarkAllAsSeenIntent(context, sortedUnreadMessages);
    PendingIntent summaryDeletePendingIntent = PendingIntent.getBroadcast(
        context,
        NOTIF_ID_BUNDLE_SUMMARY,
        markAllAsSeenIntent,
        PendingIntent.FLAG_ONE_SHOT
    );

//    String notifBody = sortedUnreadMessages.size() == 1
//        ? "From " + sortedUnreadMessages.get(0).getAuthor()
//        : "From " + sortedUnreadMessages.size() + " users";

    Notification summaryNotification = new NotificationCompat.Builder(context)
//        .setContentTitle(sortedUnreadMessages.size() + " new messages")
//        .setContentText(notifBody)
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

    for (Message unreadMessage : sortedUnreadMessages) {
      int notificationId = (int) JrawUtils.createdTimeUtc(unreadMessage);

      // Mark as read action.
      Intent markAsReadIntent = NotificationActionReceiver.createMarkAsReadIntent(context, unreadMessage, Dank.jackson(), notificationId);
      PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(context, notificationId, markAsReadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(0, "Mark read", markAsReadPendingIntent).build();

      // Direct reply action.
      Intent directReplyIntent = NotificationActionReceiver.createDirectReplyIntent(context, unreadMessage, Dank.jackson(), notificationId);
      PendingIntent directReplyPendingIntent = PendingIntent.getBroadcast(context, notificationId, directReplyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      RemoteInput directReplyInput = new RemoteInput.Builder(NotificationActionReceiver.KEY_DIRECT_REPLY_MESSAGE)
          .setLabel(context.getString(R.string.messagenotification_reply_to_user, unreadMessage.getAuthor()))
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
          .setWhen(unreadMessage.getCreated().getTime())
          .setSmallIcon(R.mipmap.ic_launcher)
          .setGroup(BUNDLED_NOTIFS_KEY)
          .setAutoCancel(true)
          .setColor(ContextCompat.getColor(context, R.color.color_accent))
          .addAction(markAsReadAction)
          .addAction(replyAction)
          .setDeleteIntent(deletePendingIntent)
          .build();
      notificationManager.notify(notificationId, bundledNotification);
    }
  }

  @CheckResult
  public Completable dismissNotification(Context context, int notificationId) {
    return Completable.fromAction(() -> {
      Timber.i("dismissNotification %s", notificationId);

      if (notificationId == -1) {
        throw new IllegalStateException();
      }

      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.cancel(notificationId);
    });
  }

  /**
   * Dismiss the summary notification of a bundle so that everything gets dismissed.
   */
  @CheckResult
  public Completable dismissAllNotifications(Context context) {
    return Completable.fromAction(() -> {
      Timber.i("Dismissing all notifs");
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.cancel(NOTIF_ID_BUNDLE_SUMMARY);
    });
  }

}
