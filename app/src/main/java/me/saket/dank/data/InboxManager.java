package me.saket.dank.data;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Single;

import android.support.annotation.CheckResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.value.AutoValue;
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
import io.reactivex.functions.Function;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.ui.user.messages.StoredMessage;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.JrawUtils;

public class InboxManager {

  /**
   * The maximum count of items that will be fetched on every pagination iteration.
   */
  public static final int MESSAGES_FETCHED_PER_PAGE = Paginator.DEFAULT_LIMIT * 2;

  private final DankRedditClient dankRedditClient;
  private final BriteDatabase briteDatabase;
  private final JacksonHelper jacksonHelper;

  public InboxManager(DankRedditClient dankRedditClient, BriteDatabase briteDatabase, JacksonHelper jacksonHelper) {
    this.dankRedditClient = dankRedditClient;
    this.briteDatabase = briteDatabase;
    this.jacksonHelper = jacksonHelper;
  }

  @CheckResult
  public Observable<List<Message>> message(InboxFolder folder) {
    return toV2Observable(briteDatabase
        .createQuery(StoredMessage.TABLE_NAME, StoredMessage.QUERY_GET_ALL_IN_FOLDER, folder.name())
        .mapToList(StoredMessage.mapMessageFromCursor(jacksonHelper)));
  }

  /**
   * Fetch more messages in <var>folder</var>.
   */
  @CheckResult
  public Single<FetchMoreResult> fetchMoreMessages(InboxFolder folder) {
    Function<PaginationAnchor, List<Message>> fetchMessages = paginationAnchor -> {
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
    };

    Consumer<List<Message>> saveFetchedMessages = fetchedMessages -> {
      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        for (Message fetchedMessage : fetchedMessages) {
          StoredMessage messageToStore = StoredMessage.create(fetchedMessage, JrawUtils.createdTimeUtc(fetchedMessage), folder);
          briteDatabase.insert(StoredMessage.TABLE_NAME, messageToStore.toContentValues(jacksonHelper));
        }
        transaction.markSuccessful();
      }
    };

    return getPaginationAnchor(folder)
        .map(fetchMessages)
        .doOnSuccess(saveFetchedMessages)
        .map(fetchedMessages -> FetchMoreResult.create(fetchedMessages.isEmpty()));
  }

  /**
   * Create a PaginationAnchor from the last message in <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> getPaginationAnchor(InboxFolder folder) {
    StoredMessage dummyDefaultValue = StoredMessage.create(new PrivateMessage(null), 0, InboxFolder.PRIVATE_MESSAGES);

    return toV2Single(briteDatabase.createQuery(StoredMessage.TABLE_NAME, StoredMessage.QUERY_GET_LAST_IN_FOLDER, folder.name())
        .mapToOneOrDefault(StoredMessage.mapFromCursor(jacksonHelper), dummyDefaultValue)
        .take(1)
        .map(lastStoredMessage -> {
          if (lastStoredMessage == dummyDefaultValue) {
            return PaginationAnchor.createEmpty();

          } else {
            Message lastMessage = lastStoredMessage.message();

            // Private messages can have nested replies. Go through them and find the last one.
            if (lastMessage instanceof PrivateMessage) {
              JsonNode repliesNode = lastMessage.getDataNode().get("replies");

              if (repliesNode.isObject()) {
                // Replies are present.
                //noinspection MismatchedQueryAndUpdateOfCollection
                Listing<Message> lastMessageReplies = new Listing<>(repliesNode.get("data"), Message.class);
                Message lastMessageLastReply = lastMessageReplies.get(lastMessageReplies.size() - 1);
                return PaginationAnchor.create(lastMessageLastReply.getFullName());
              }
            }

            return PaginationAnchor.create(lastMessage.getFullName());
          }
        })
        .toSingle());
  }

  @AutoValue
  public abstract static class FetchMoreResult {
    public abstract boolean wasEmpty();

    public static FetchMoreResult create(boolean empty) {
      return new AutoValue_InboxManager_FetchMoreResult(empty);
    }

  }

// ======== READ STATUS ======== //

  @CheckResult
  public Completable setRead(Message message, boolean read) {
    return Completable.fromAction(() -> dankRedditClient.redditInboxManager().setRead(read, message));
  }

  @CheckResult
  public Completable setAllRead() {
    return Completable.fromAction(() -> dankRedditClient.redditInboxManager().setAllRead());
  }

}
