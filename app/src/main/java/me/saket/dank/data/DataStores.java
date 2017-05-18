package me.saket.dank.data;

import com.nytimes.android.external.fs2.filesystem.FileSystem;
import com.nytimes.android.external.store2.base.impl.MemoryPolicy;
import com.nytimes.android.external.store2.base.impl.Store;

import me.saket.dank.utils.JacksonHelper;

/**
 * Repository for all {@link Store} we have in Dank.
 */
public class DataStores {

    /**
     * The maximum count of items that will be fetched on every pagination iteration.
     */
    public static final int MESSAGES_FETCHED_PER_PAGE = InboxManager.MESSAGES_FETCHED_PER_PAGE;

    // TODO: Use custom caching policy for each store. Messages should keep the cache forever.
    public DataStores(DankRedditClient dankRedditClient, JacksonHelper jacksonHelper, FileSystem cacheFileSystem, MemoryPolicy cachingPolicy) {
    }

}
