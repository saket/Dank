package me.saket.dank.ui.user.messages;

import android.support.annotation.StringRes;

import net.dean.jraw.references.InboxReference;

import me.saket.dank.R;

public enum InboxFolder {

  UNREAD(R.string.inbox_tab_unread, "unread"),
  PRIVATE_MESSAGES(R.string.inbox_tab_private_messages, "messages"),
  COMMENT_REPLIES(R.string.inbox_tab_comment_replies, "inbox"),
  POST_REPLIES(R.string.inbox_tab_post_replies, "inbox"),
  USERNAME_MENTIONS(R.string.inbox_tab_username_mentions, "mentions"),;

  public static InboxFolder[] ALL = { UNREAD, PRIVATE_MESSAGES, COMMENT_REPLIES, POST_REPLIES, USERNAME_MENTIONS };

  private final int titleRes;

  /**
   * Value used by {@link InboxReference#iterate(String)}.
   */
  public final String value;

  InboxFolder(@StringRes int titleRes, String value) {
    this.titleRes = titleRes;
    this.value = value;
  }

  public int titleRes() {
    return titleRes;
  }
}
