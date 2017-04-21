package me.saket.dank.ui.user.messages;

import android.support.annotation.StringRes;

import net.dean.jraw.paginators.InboxPaginator;

import me.saket.dank.R;

public enum MessageFolder {
    UNREAD(R.string.messages_tab_unread, "unread"),
    PRIVATE_MESSAGES(R.string.messages_tab_messages, "messages"),
    //COMMENT_REPLIES,
    //POST_REPLIES,
    USERNAME_MENTIONS(R.string.messages_tab_username_mentions, "mentions"),;

    private final int titleRes;
    private final String value;

    /**
     * @param value Used by {@link InboxPaginator}.
     */
    MessageFolder(@StringRes int titleRes, String value) {
        this.titleRes = titleRes;
        this.value = value;
    }

    public int titleRes() {
        return titleRes;
    }

    public String value() {
        return value;
    }

}
