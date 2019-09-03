package me.saket.dank.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.pagination.Paginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import me.saket.dank.notifs.MessageNotifActionsJobService;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.user.messages.CachedMessage;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;

import static java.util.Collections.unmodifiableList;

@Singleton
public class InboxRepository {

  /**
   * The maximum count of items that will be fetched on every pagination iteration.
   */
  public static final int MESSAGES_FETCHED_PER_PAGE = Paginator.DEFAULT_LIMIT * 2;

  private final Lazy<Reddit> reddit;
  private final BriteDatabase briteDatabase;
  private final Lazy<MoshiAdapter> moshiAdapter;
  private final ReplyRepository replyRepository;

  @Inject
  public InboxRepository(Lazy<Reddit> reddit, BriteDatabase briteDatabase, Lazy<MoshiAdapter> moshiAdapter, ReplyRepository replyRepository) {
    this.reddit = reddit;
    this.briteDatabase = briteDatabase;
    this.moshiAdapter = moshiAdapter;
    this.replyRepository = replyRepository;
  }

  /**
   * Stream of all messages in <var>folder</var>
   */
  @CheckResult
  public Observable<List<Message>> messages(InboxFolder folder) {
    return briteDatabase
        .createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_ALL_IN_FOLDER, folder.name())
        .mapToList(CachedMessage.messageFromCursor(moshiAdapter.get()))
        .as(Arrays2.immutable());
  }

  /**
   * Stream of message and its child replies. The type is optional because messages in unread might have not
   * been downloaded yet in private-messages folder.
   * <p>
   * Both fullname and folder are required because {@link CachedMessage} uses a composite key of the fullname
   * and the folder. This is because it's possible for the same message to be present in Unread as well as
   * Private Message folder.
   */
  @CheckResult
  public Observable<Optional<Message>> messages(String fullname, InboxFolder folder) {
    return briteDatabase
        .createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_SINGLE, fullname, folder.name())
        .mapToOneOrDefault(CachedMessage.optionalMessageFromCursor(moshiAdapter.get()), Optional.empty());
  }

  /**
   * Fetch messages after the oldest message we locally have in <var>folder</var>.
   */
  @CheckResult
  public Single<List<Message>> fetchAndSaveMoreMessages(InboxFolder folder) {
    return getPaginationAnchor(folder)
        .flatMap(anchor -> fetchMessagesFromAnchor(folder, anchor))
        .doOnSuccess(saveMessages(folder, false))
        .map(fetchedMessages -> unmodifiableList(fetchedMessages));
  }

  /**
   * Fetch messages after the oldest message we locally have in <var>folder</var>.
   */
  @CheckResult
  public Single<FetchAndSaveResult> fetchAndSaveMoreMessagesWithResult(InboxFolder folder) {
    return fetchAndSaveMoreMessages(folder)
        .map(FetchAndSaveResult::success)
        .onErrorReturn(FetchAndSaveResult::error);
  }

  /**
   * Fetch most recent messages. Unlike {@link #fetchAndSaveMoreMessages(InboxFolder)},
   * this does not use the oldest message as the anchor.
   */
  @CheckResult
  public Single<List<Message>> refreshMessages(InboxFolder folder, boolean removeExistingMessages) {
    return fetchMessagesFromAnchor(folder, PaginationAnchor.createEmpty())
        .doOnSuccess(saveMessages(folder, removeExistingMessages))
        .flatMap(messages -> {
          if (folder == InboxFolder.PRIVATE_MESSAGES) {
            return Observable.fromIterable(messages)
                .map(message -> ParentThread.of(message))
                .concatMapEager(parentThread -> replyRepository.removeSyncPendingPostedReplies(parentThread).toObservable())
                .ignoreElements()
                .toSingleDefault(messages);
          } else {
            return Single.just(messages);
          }
        })
        .map(fetchedMessages -> unmodifiableList(fetchedMessages));
  }

  @CheckResult
  private Single<List<Message>> fetchMessagesFromAnchor(InboxFolder folder, PaginationAnchor paginationAnchor) {
    return reddit.get().loggedInUser()
        .messages(folder, MESSAGES_FETCHED_PER_PAGE, paginationAnchor)
        .map(iterator -> {
          // Try fetching a minimum of 10 items. Useful for comment and
          // post replies where we have to filter the messages locally.
          List<Message> minimum10Messages = new ArrayList<>();

          while (iterator.hasNext() && minimum10Messages.size() < 10) {
            // iterator.next() makes an API call.
            Listing<Message> nextSetOfMessages = iterator.next();

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
        });
  }

  /**
   * Create a PaginationAnchor from the last message in <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> getPaginationAnchor(InboxFolder folder) {
    return briteDatabase.createQuery(CachedMessage.TABLE_NAME, CachedMessage.QUERY_GET_LAST_IN_FOLDER, folder.name())
        .mapToList(CachedMessage.fromCursor(moshiAdapter.get()))
        .map(items -> items.isEmpty()
            ? Collections.singletonList(Optional.<CachedMessage>empty())
            : Collections.singletonList(Optional.of(items.get(0))))
        .flatMapIterable(items -> items)
        .firstOrError()
        .map(optionalLastStoredMessage -> {
          if (optionalLastStoredMessage.isEmpty()) {
            return PaginationAnchor.createEmpty();
          } else {
            Message lastMessage = optionalLastStoredMessage.get().message();

            // Private messages can have nested replies. Go through them and find the last one.
            if (!lastMessage.isComment()) {
              List<Message> lastMessageReplies = JrawUtils2.messageReplies(lastMessage);
              //noinspection ConstantConditions
              if (!lastMessageReplies.isEmpty()) {
                // Replies are present.
                Message lastMessageLastReply = lastMessageReplies.get(lastMessageReplies.size() - 1);
                return PaginationAnchor.create(lastMessageLastReply.getFullName());
              }
            }

            return PaginationAnchor.create(lastMessage.getFullName());
          }
        });
  }

  /**
   * @param removeExistingMessages Whether to remove existing messages under <var>folder</var>.
   */
  private Consumer<List<Message>> saveMessages(InboxFolder folder, boolean removeExistingMessages) {
    return fetchedMessages -> {
      List<ContentValues> messagesValuesToStore = new ArrayList<>(fetchedMessages.size());
      for (Message fetchedMessage : fetchedMessages) {
        long latestMessageTimestamp;
        if (fetchedMessage.isComment()) {
          latestMessageTimestamp = fetchedMessage.getCreated().getTime();
        } else {
          List<Message> messageReplies = JrawUtils2.messageReplies(fetchedMessage);
          Message latestMessage = messageReplies.isEmpty() ? fetchedMessage : messageReplies.get(messageReplies.size() - 1);
          latestMessageTimestamp = latestMessage.getCreated().getTime();
        }
        CachedMessage cachedMessage = CachedMessage.create(fetchedMessage.getFullName(), fetchedMessage, latestMessageTimestamp, folder);
        messagesValuesToStore.add(cachedMessage.toContentValues(moshiAdapter.get()));
      }

      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        if (removeExistingMessages) {
          briteDatabase.delete(CachedMessage.TABLE_NAME, CachedMessage.WHERE_FOLDER, folder.name());
        }
        for (ContentValues cachedMessageValues : messagesValuesToStore) {
          briteDatabase.insert(CachedMessage.TABLE_NAME, cachedMessageValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
        transaction.markSuccessful();
      }
    };
  }

  @CheckResult
  private Completable removeMessages(InboxFolder folder, Identifiable... messages) {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        for (Identifiable message : messages) {
          briteDatabase.delete(CachedMessage.TABLE_NAME, CachedMessage.WHERE_FOLDER_AND_FULLNAME, folder.name(), message.getFullName());
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

  /**
   * Access via {@link MessageNotifActionsJobService#markAsRead(Context, MoshiAdapter, Message...)}.
   */
  @CheckResult
  public Completable setRead(Identifiable[] messages, boolean read) {
    return reddit.get().loggedInUser().setMessagesRead(read, messages)
        .andThen(removeMessages(InboxFolder.UNREAD, messages));
  }

  /**
   * Access via {@link MessageNotifActionsJobService#markAsRead(Context, MoshiAdapter, Message...)}.
   */
  @CheckResult
  public Completable setRead(Identifiable message, boolean read) {
    return reddit.get().loggedInUser().setMessagesRead(read, message)
        .andThen(removeMessages(InboxFolder.UNREAD, message));
  }

  /**
   * Access via {@link MessageNotifActionsJobService#markAllAsRead(Context)}.
   */
  @CheckResult
  public Completable setAllRead() {
    return reddit.get().loggedInUser().setAllMessagesRead()
        .andThen(removeAllMessages(InboxFolder.UNREAD));
  }

  public interface FetchAndSaveResult {

    static FetchAndSaveResult success(List<Message> fetchedMessages) {
      return new AutoValue_InboxRepository_FetchAndSaveResult_Success(fetchedMessages);
    }

    static FetchAndSaveResult error(Throwable error) {
      return new AutoValue_InboxRepository_FetchAndSaveResult_GenericError(error);
    }

    @AutoValue
    abstract class Success implements FetchAndSaveResult {
      public abstract List<Message> fetchedMessages();
    }

    @AutoValue
    abstract class GenericError implements FetchAndSaveResult {
      public abstract Throwable error();
    }
  }
}
