package me.saket.dank.ui.user.messages;

import android.content.Context;
import dagger.Lazy;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import me.saket.dank.R;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Optional;
import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Message;

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
      boolean constructThreads)
  {
    return messagesStream
        .map(messages -> {
          List<InboxFolderScreenUiModel> models = new ArrayList<>(messages.size());
          String loggedInUserName = userSessionRepo.get().loggedInUserName();
          for (Message message : messages) {
            if (constructThreads) {
              models.add(messageThreadUiModel(c, message, loggedInUserName));
            } else {
              models.add(individualMessageUiModel(c, message));
            }
          }
          return models;
        });
  }

  private InboxIndividualMessage.UiModel individualMessageUiModel(Context c, Message message) {
    String linkTitle = message.isComment()
        ? ((CommentMessage) message).getLinkTitle()
        : message.getSubject();

    Optional<String> from;
    if (message.isComment()) {
      from = Optional.of(c.getResources().getString(R.string.subreddit_name_r_prefix, message.getSubreddit()));
    } else {
      from = Optional.empty();
    }

    long adapterId = message.getId().hashCode();
    String authorName = message.getAuthor();
    String timestamp = Dates.createTimestamp(c.getResources(), JrawUtils.createdTimeUtc(message)).toString();
    CharSequence body = markdown.get().parse(message);
    return InboxIndividualMessage.UiModel.create(adapterId, linkTitle, timestamp, authorName, from, body, message);
  }

  private InboxMessageThread.UiModel messageThreadUiModel(Context c, Message messageThread, String loggedInUserName) {
    List<Message> replies = JrawUtils.messageReplies(messageThread);
    Message latestMessageInThread = replies.isEmpty()
        ? messageThread
        : replies.get(replies.size() - 1);

    Optional<String> secondPartyName = Optional.ofNullable(JrawUtils.secondPartyName(
        c.getResources(),
        latestMessageInThread,
        loggedInUserName));

    String snippetWithEscapedHtml = JrawUtils.messageBodyHtml(latestMessageInThread);
    String snippet = markdown.get().stripMarkdown(snippetWithEscapedHtml).replace("\n", " ");

    boolean wasLastMessageBySelf = loggedInUserName.equalsIgnoreCase(latestMessageInThread.getAuthor());  // Author can be null.
    snippet = wasLastMessageBySelf
        ? c.getResources().getString(R.string.inbox_snippet_sent_by_logged_in_user, snippet)
        : snippet;

    long adapterId = messageThread.getId().hashCode();
    String timestamp = Dates.createTimestamp(c.getResources(), JrawUtils.createdTimeUtc(latestMessageInThread)).toString();
    return InboxMessageThread.UiModel.create(adapterId, secondPartyName, messageThread.getSubject(), snippet, timestamp, messageThread);
  }
}
