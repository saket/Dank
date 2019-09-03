package me.saket.dank.ui.user.messages;

import android.content.Context;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.markdown.Markdown;

public class InboxFolderUiConstructor {

  private final Lazy<Markdown> markdown;
  private final Lazy<UserSessionRepository> userSessionRepo;

  @Inject
  public InboxFolderUiConstructor(Lazy<Markdown> markdown, Lazy<UserSessionRepository> userSessionRepo) {
    this.markdown = markdown;
    this.userSessionRepo = userSessionRepo;
  }

  public Observable<List<InboxFolderScreenUiModel>> stream(
      Context c,
      Observable<List<Message>> messagesStream,
      boolean constructThreads,
      boolean isUnreadFolder)
  {
    return messagesStream
        .map(messages -> {
          List<InboxFolderScreenUiModel> models = new ArrayList<>(messages.size());
          String loggedInUserName = userSessionRepo.get().loggedInUserName();
          for (Message message : messages) {
            if (constructThreads) {
              models.add(messageThreadUiModel(c, message, loggedInUserName));
            } else {
              models.add(individualMessageUiModel(c, message, isUnreadFolder));
            }
          }
          return models;
        });
  }

  /**
   * IMPORTANT: Keep the identification of these details like title, author, etc., in sync with MessagesNotificationManager.
   */
  private InboxIndividualMessage.UiModel individualMessageUiModel(Context c, Message message, boolean isUnreadFolder) {
    InboxMessageType messageType = InboxMessageType.parse(message);

    String title;
    String byline;
    String senderInformation;
    String timestamp = Dates.createTimestamp(c.getResources(), message.getCreated().getTime()).toString();
    String subredditName = c.getString(R.string.subreddit_name_r_prefix, message.getSubreddit());

    switch (messageType) {
      case COMMENT_REPLY:
        title = message.getLinkTitle();
        byline = isUnreadFolder
            ? c.getString(R.string.inbox_message_byline_for_unread_folder_comment_reply, timestamp)
            : timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_comment_reply, message.getAuthor(), subredditName);
        break;

      case USERNAME_MENTION:
        title = message.getLinkTitle();
        byline = isUnreadFolder
            ? c.getString(R.string.inbox_message_byline_for_unread_folder_username_mention, timestamp)
            : timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_username_mention, message.getAuthor(), subredditName);
        break;

      case POST_REPLY:
        title = message.getSubject();
        byline = isUnreadFolder
            ? c.getString(R.string.inbox_message_byline_for_unread_folder_post_reply, timestamp)
            : timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_comment_reply, message.getAuthor(), subredditName);
        break;

      case SUBREDDIT_MESSAGE:
        title = message.getSubject();
        byline = isUnreadFolder
            ? c.getString(R.string.inbox_message_byline_for_unread_folder_subreddit_message, timestamp)
            : timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_subreddit_message, subredditName);
        break;

      case PRIVATE_MESSAGE:
        title = message.getSubject();
        byline = isUnreadFolder
            ? c.getString(R.string.inbox_message_byline_for_unread_folder_private_message, timestamp)
            : timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_private_message, message.getAuthor());
        break;

      default:
      case UNKNOWN:
        title = message.getSubject();
        byline = timestamp;
        senderInformation = c.getString(R.string.inbox_message_sender_info_for_private_message, message.getAuthor());
        break;
    }

    long adapterId = JrawUtils2.generateAdapterId(message);
    CharSequence body = markdown.get().parse(message);
    //noinspection ConstantConditions
    return InboxIndividualMessage.UiModel.create(adapterId, title, byline, senderInformation, body, message);
  }

  private InboxMessageThread.UiModel messageThreadUiModel(Context c, Message messageThread, String loggedInUserName) {
    List<Message> replies = JrawUtils2.messageReplies(messageThread);
    Message latestMessageInThread = replies.isEmpty()
        ? messageThread
        : replies.get(replies.size() - 1);

    Optional<String> secondPartyName = Optional.ofNullable(JrawUtils2.secondPartyName(
        c.getResources(),
        messageThread,
        loggedInUserName));

    String snippet = markdown.get().stripMarkdown(latestMessageInThread).replace("\n", " ");

    boolean wasLastMessageBySelf = loggedInUserName.equalsIgnoreCase(latestMessageInThread.getAuthor());  // Author can be null.
    snippet = wasLastMessageBySelf
        ? c.getResources().getString(R.string.inbox_snippet_sent_by_logged_in_user, snippet)
        : snippet;

    long adapterId = JrawUtils2.generateAdapterId(messageThread);
    String timestamp = Dates.createTimestamp(c.getResources(), latestMessageInThread.getCreated().getTime()).toString();
    return InboxMessageThread.UiModel.create(adapterId, secondPartyName, messageThread.getSubject(), snippet, timestamp, messageThread);
  }
}
