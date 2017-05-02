package me.saket.dank.ui.user.messages;

import com.google.auto.value.AutoValue;
import com.nytimes.android.external.fs2.PathResolver;

import me.saket.dank.data.PaginationAnchor;

@AutoValue
public abstract class MessageCacheKey {

    public abstract InboxFolder folder();

    public abstract PaginationAnchor paginationAnchor();

    public static MessageCacheKey create(InboxFolder folder, PaginationAnchor startAfterThingName) {
        return new AutoValue_MessageCacheKey(folder, startAfterThingName);
    }

    public boolean hasPaginationAnchor() {
        return paginationAnchor() != null;
    }

    public static PathResolver<MessageCacheKey> PATH_RESOLVER = key -> {
        //noinspection CodeBlock2Expr
        return key.folder().value() + key.paginationAnchor().fullName();
    };

}
