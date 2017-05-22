package me.saket.dank.ui.user.messages;

import android.support.annotation.StringRes;

import net.dean.jraw.paginators.InboxPaginator;

import me.saket.dank.R;

public enum InboxFolder {

  UNREAD(R.string.inbox_tab_unread, "unread"),
  PRIVATE_MESSAGES(R.string.inbox_tab_private_messages, "messages"),
  COMMENT_REPLIES(R.string.inbox_tab_comment_replies, "inbox"),
  POST_REPLIES(R.string.inbox_tab_post_replies, "inbox"),
  USERNAME_MENTIONS(R.string.inbox_tab_username_mentions, "mentions"),;

  public static InboxFolder[] ALL = { UNREAD, PRIVATE_MESSAGES, COMMENT_REPLIES, POST_REPLIES, USERNAME_MENTIONS };

  private final int titleRes;
  private final String value;

  /**
   * @param value Used by {@link InboxPaginator}.
   */
  InboxFolder(@StringRes int titleRes, String value) {
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
