package me.saket.dank.data;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.nytimes.android.external.fs2.filesystem.FileSystem;
import com.nytimes.android.external.store2.base.Fetcher;
import com.nytimes.android.external.store2.base.Persister;
import com.nytimes.android.external.store2.base.impl.MemoryPolicy;
import com.nytimes.android.external.store2.base.impl.Store;
import com.nytimes.android.external.store2.base.impl.StoreBuilder;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import me.saket.dank.ui.user.messages.MessageCacheKey;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.StoreFilePersister;

/**
 * Repository for all {@link Store} we have in Dank.
 */
public class DataStores {

    private final Store<List<Message>, MessageCacheKey> messageStore;

    public DataStores(DankRedditClient dankRedditClient, JacksonHelper jacksonHelper, FileSystem cacheFileSystem, MemoryPolicy cachingPolicy) {
        messageStore = createMessageStore(dankRedditClient, jacksonHelper, cacheFileSystem, cachingPolicy);
    }

    private Store<List<Message>, MessageCacheKey> createMessageStore(DankRedditClient dankRedditClient, JacksonHelper jacksonHelper,
            FileSystem cacheFileSystem, MemoryPolicy cachingPolicy)
    {
        Fetcher<List<Message>, MessageCacheKey> networkFetcher = key -> {
            Callable<List<Message>> paginatorCallable = () -> {
                InboxPaginator paginator = dankRedditClient.userMessagePaginator(key.folder());
                paginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

                if (key.hasPaginationAnchor()) {
                    //noinspection ConstantConditions
                    paginator.setStartAfterThing(key.paginationAnchor().fullName());
                }

                // Try fetching a minimum of 10 comment replies.
                List<Message> minimum10Messages = new ArrayList<>();

                while (paginator.hasNext()) {
                    // paginator.next() makes an API call.
                    Listing<Message> nextSetOfMessages = paginator.next();

                    for (Message nextMessage : nextSetOfMessages) {
                        switch (key.folder()) {
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

                    if (minimum10Messages.size() > 10) {
                        break;
                    }
                }

                return minimum10Messages;
            };

            return dankRedditClient.withAuth(Single.fromCallable(paginatorCallable));
        };

        StoreFilePersister.JsonParser<List<Message>> diskPersistJsonParser = new StoreFilePersister.JsonParser<List<Message>>() {
            @Override
            public String toJson(List<Message> messagesToPersist) {
                JsonNode[] jsonNodes = new JsonNode[messagesToPersist.size()];
                for (int i = 0; i < messagesToPersist.size(); i++) {
                    jsonNodes[i] = (messagesToPersist.get(i).getDataNode());
                }
                return jacksonHelper.toJson(jsonNodes);
            }

            @Override
            @SuppressWarnings("ConstantConditions")
            public List<Message> fromJson(InputStream jsonInputStream) {
                JsonNode[] jsonNodes = jacksonHelper.fromJson(jsonInputStream, JsonNode[].class);
                return constructMessagesFromJsonNodes(jsonNodes);
            }

            @NonNull
            private List<Message> constructMessagesFromJsonNodes(JsonNode[] jsonNodes) {
                List<Message> deserializedMessages = new ArrayList<>(jsonNodes.length);
                for (JsonNode jsonNode : jsonNodes) {
                    boolean isCommentMessage = jsonNode.get("was_comment").asBoolean();
                    Message deserializedMessage = isCommentMessage ? new CommentMessage(jsonNode) : new PrivateMessage(jsonNode);
                    deserializedMessages.add(deserializedMessage);
                }
                return deserializedMessages;
            }
        };

        Persister<List<Message>, MessageCacheKey> diskPersister = new StoreFilePersister<>(
                cacheFileSystem,
                MessageCacheKey.PATH_RESOLVER,
                diskPersistJsonParser,
                cachingPolicy.getExpireAfter(),
                cachingPolicy.getExpireAfterTimeUnit()
        );

        return StoreBuilder.<MessageCacheKey, List<Message>>key()
                .fetcher(networkFetcher)
                .persister(diskPersister)
                .memoryPolicy(cachingPolicy)
                .open();
    }

    public Store<List<Message>, MessageCacheKey> messageStore() {
        return messageStore;
    }

}
