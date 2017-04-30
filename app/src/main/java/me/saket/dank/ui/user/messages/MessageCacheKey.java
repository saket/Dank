package me.saket.dank.ui.user.messages;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.nytimes.android.external.fs2.PathResolver;

import me.saket.dank.data.PaginationAnchor;

@AutoValue
public abstract class MessageCacheKey {

    public abstract InboxFolder folder();

    @Nullable
    public abstract PaginationAnchor paginationAnchor();

    public static MessageCacheKey create(InboxFolder folder, PaginationAnchor startAfterThingName) {
        return new AutoValue_MessageCacheKey(folder, startAfterThingName);
    }

    public boolean hasPaginationAnchor() {
        return paginationAnchor() != null;
    }

    public static PathResolver<MessageCacheKey> PATH_RESOLVER = key -> {
        if (key.hasPaginationAnchor()) {
            //noinspection ConstantConditions
            return key.folder().name() + key.paginationAnchor().fullName();
        } else {
            return key.folder().name();
        }
    };

}
