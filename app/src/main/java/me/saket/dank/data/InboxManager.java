package me.saket.dank.data;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Single;
import static java.util.Collections.unmodifiableList;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import me.saket.dank.ui.user.messages.CachedMessage;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.JrawUtils;

public class InboxManager {

  /**
   * The maximum count of items that will be fetched on every pagination iteration.
   */
  public static final int MESSAGES_FETCHED_PER_PAGE = Paginator.DEFAULT_LIMIT * 2;

  private final DankRedditClient dankRedditClient;
  private final BriteDatabase briteDatabase;
  private Moshi moshi;

  public InboxManager(DankRedditClient dankRedditClient, BriteDatabase briteDatabase, Moshi moshi) {
    this.dankRedditClient = dankRedditClient;
    this.briteDatabase = briteDatabase;
    this.moshi = moshi;
  }

  /**
   * Stream of all messages in <var>folder</var>
   */
  @CheckResult
  public Observable<List<Message>> messages(InboxFolder folder) {
    return toV2Observable(briteDatabase
        .createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_ALL_IN_FOLDER, folder.name())
        .mapToList(CachedMessage.mapMessageFromCursor(moshi)))
        .map(mutableList -> unmodifiableList(mutableList));
  }

  /**
   * Both ID and folder are required because {@link CachedMessage} uses a composite key of the ID and the
   * folder. This is because it's possible for the same message to be present in Unread as well as Private
   * Message folder.
   */
  @CheckResult
  public Observable<Message> message(String messageId, InboxFolder folder) {
    return toV2Observable(briteDatabase
        .createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_SINGLE, messageId, folder.name())
        .mapToOne(CachedMessage.mapMessageFromCursor(moshi)));
  }

  /**
   * Fetch messages after the oldest message we locally have in <var>folder</var>.
   */
  @CheckResult
  public Single<List<Message>> fetchMoreMessages(InboxFolder folder) {
    return getPaginationAnchor(folder)
        .flatMap(anchor -> fetchMessagesFromAnchor(folder, anchor))
        .doOnSuccess(saveMessages(folder, false))
        .map(fetchedMessages -> unmodifiableList(fetchedMessages));
  }

  /**
   * Fetch most recent messages. Unlike {@link #fetchMoreMessages(InboxFolder)},
   * this does not use the oldest message as the anchor.
   */
  @CheckResult
  public Single<List<Message>> refreshMessages(InboxFolder folder, boolean replaceAllMessages) {
    return fetchMessagesFromAnchor(folder, PaginationAnchor.createEmpty())
        .doOnSuccess(saveMessages(folder, replaceAllMessages))
        .map(fetchedMessages -> unmodifiableList(fetchedMessages));
  }

  @CheckResult
  private Single<List<Message>> fetchMessagesFromAnchor(InboxFolder folder, PaginationAnchor paginationAnchor) {
    return dankRedditClient.withAuth(Single.fromCallable(() -> {
      InboxPaginator paginator = dankRedditClient.userMessagePaginator(folder);
      paginator.setLimit(MESSAGES_FETCHED_PER_PAGE);

      if (!paginationAnchor.isEmpty()) {
        paginator.setStartAfterThing(paginationAnchor.fullName());
      }

      // Try fetching a minimum of 10 items. Useful for comment and post replies
      // where we have to filter the messages locally.
      List<Message> minimum10Messages = new ArrayList<>();

      while (paginator.hasNext() && minimum10Messages.size() < 10) {
        // paginator.next() makes an API call.
        Listing<Message> nextSetOfMessages = paginator.next();

        for (Message nextMessage : nextSetOfMessages) {
          switch (folder) {
            case UNREAD:
            case PRIVATE_MESSAGES:
            case USERNAME_MENTIONS:
              minimum10Messages.add(nextMessage);
              break;

            case COMMENT_REPLIES:
              if ("comment reply".equals(nextMessage.getSubject())) {
                minimum10Messages.add(nextMessage);
              }
              break;

            case POST_REPLIES:
              if ("post reply".equals(nextMessage.getSubject())) {
                minimum10Messages.add(nextMessage);
              }
              break;

            default:
              throw new UnsupportedOperationException();
          }
        }
      }

      return minimum10Messages;
    }));
  }

  /**
   * Create a PaginationAnchor from the last message in <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> getPaginationAnchor(InboxFolder folder) {
    CachedMessage dummyDefaultValue = CachedMessage.create("-1", new PrivateMessage(null), 0, InboxFolder.PRIVATE_MESSAGES);

    return toV2Single(briteDatabase.createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_LAST_IN_FOLDER, folder.name())
        .mapToOneOrDefault(CachedMessage.mapFromCursor(moshi), dummyDefaultValue)
        .first()
        .map(lastStoredMessage -> {
          if (lastStoredMessage == dummyDefaultValue) {
            return PaginationAnchor.createEmpty();

          } else {
            Message lastMessage = lastStoredMessage.message();

            // Private messages can have nested replies. Go through them and find the last one.
            if (lastMessage instanceof PrivateMessage) {
              List<Message> lastMessageReplies = JrawUtils.messageReplies(lastMessage);
              if (!lastMessageReplies.isEmpty()) {
                // Replies are present.
                Message lastMessageLastReply = lastMessageReplies.get(lastMessageReplies.size() - 1);
                return PaginationAnchor.create(lastMessageLastReply.getFullName());
              }
            }

            return PaginationAnchor.create(lastMessage.getFullName());
          }
        })
        .toSingle());
  }

  /**
   * @param removeExistingMessages Whether to remove existing messages under <var>folder</var>.
   */
  private Consumer<List<Message>> saveMessages(InboxFolder folder, boolean removeExistingMessages) {
    return fetchedMessages -> {
      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        if (removeExistingMessages) {
          briteDatabase.delete(CachedMessage.TABLE_NAME, CachedMessage.WHERE_FOLDER, folder.name());
        }

        for (Message fetchedMessage : fetchedMessages) {
          long latestMessageTimestamp;
          if (fetchedMessage.isComment()) {
            latestMessageTimestamp = JrawUtils.createdTimeUtc(fetchedMessage);
          } else {
            List<Message> messageReplies = JrawUtils.messageReplies(fetchedMessage);
            Message latestMessage = messageReplies.isEmpty() ? fetchedMessage : messageReplies.get(messageReplies.size() - 1);
            latestMessageTimestamp = JrawUtils.createdTimeUtc(latestMessage);
          }
          CachedMessage messageToStore = CachedMessage.create(fetchedMessage.getId(), fetchedMessage, latestMessageTimestamp, folder);
          briteDatabase.insert(CachedMessage.TABLE_NAME, messageToStore.toContentValues(moshi), SQLiteDatabase.CONFLICT_REPLACE);
        }
        transaction.markSuccessful();
      }
    };
  }

  @CheckResult
  private Completable removeMessages(InboxFolder folder, Message... messages) {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        for (Message message : messages) {
          briteDatabase.delete(CachedMessage.TABLE_NAME, CachedMessage.WHERE_FOLDER_AND_ID, folder.name(), message.getId());
        }
        transaction.markSuccessful();
      }
    });
  }

  @CheckResult
  private Completable removeAllMessages(InboxFolder folder) {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        briteDatabase.delete(CachedMessage.TABLE_NAME, CachedMessage.WHERE_FOLDER, folder.name());
        transaction.markSuccessful();
      }
    });
  }

// ======== READ STATUS ======== //

  @CheckResult
  public Completable setRead(Message[] messages, boolean read) {
    return Completable.fromAction(() -> dankRedditClient.redditInboxManager().setRead(read, messages[0], messages))
        .andThen(removeMessages(InboxFolder.UNREAD, messages));
  }

  @CheckResult
  public Completable setAllRead() {
    return Completable.fromAction(() -> dankRedditClient.redditInboxManager().setAllRead())
        .andThen(removeAllMessages(InboxFolder.UNREAD));
  }

}
