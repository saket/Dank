package me.saket.dank.data;

import com.nytimes.android.external.fs2.filesystem.FileSystem;
import com.nytimes.android.external.store2.base.Fetcher;
import com.nytimes.android.external.store2.base.impl.MemoryPolicy;
import com.nytimes.android.external.store2.base.impl.Store;
import com.nytimes.android.external.store2.base.impl.StoreBuilder;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.util.List;

import io.reactivex.Single;
import me.saket.dank.ui.user.messages.MessageCacheKey;
import timber.log.Timber;

/**
 * Repository for all {@link Store} we have in Dank.
 */
public class DataStores {

    private final Store<List<Message>, MessageCacheKey> messageStore;

    public DataStores(DankRedditClient dankRedditClient, Moshi moshi, FileSystem cacheFileSystem, MemoryPolicy cachingPolicy) {
        messageStore = createMessageStore(dankRedditClient, moshi, cacheFileSystem, cachingPolicy);
    }

    private Store<List<Message>, MessageCacheKey> createMessageStore(DankRedditClient dankRedditClient, Moshi moshi, FileSystem cacheFileSystem,
            MemoryPolicy cachingPolicy)
    {
        Fetcher<List<Message>, MessageCacheKey> networkFetcher = key -> Single.fromCallable(() -> {
            InboxPaginator paginator = dankRedditClient.userMessagePaginator(key.folder());
            paginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

            if (key.hasPaginationAnchor()) {
                //noinspection ConstantConditions
                paginator.setStartAfterThing(key.paginationAnchor().fullName());
            }

            Timber.i("Fetching messages in %s from remote. Anchor: %s", key.folder(), key.paginationAnchor());
            return paginator.next(true);
        });

//        Persister<List<Message>, MessageCacheKey> diskPersister = new StoreFilePersister<>(
//                cacheFileSystem,
//                MessageCacheKey.PATH_RESOLVER,
//                moshi,
//                Types.newParameterizedType(List.class, Message.class),
//                cachingPolicy.getExpireAfter(),
//                cachingPolicy.getExpireAfterTimeUnit()
//        );

        return StoreBuilder.<MessageCacheKey, List<Message>>key()
                .fetcher(networkFetcher)
                //.persister(diskPersister)
                .memoryPolicy(cachingPolicy)
                .open();
    }

    public Store<List<Message>, MessageCacheKey> messageStore() {
        return messageStore;
    }

}
