package me.saket.dank.ui.user.messages

import androidx.annotation.StringRes
import me.saket.dank.R
import net.dean.jraw.references.InboxReference

enum class InboxFolder(
    @StringRes private val titleRes: Int,

    /** Value used by [InboxReference.iterate]. */
    val value: String
) {

  UNREAD(R.string.inbox_tab_unread, "unread"),
  PRIVATE_MESSAGES(R.string.inbox_tab_private_messages, "messages"),
  COMMENT_REPLIES(R.string.inbox_tab_comment_replies, "inbox"),
  POST_REPLIES(R.string.inbox_tab_post_replies, "inbox"),
  USERNAME_MENTIONS(R.string.inbox_tab_username_mentions, "mentions");

  fun titleRes(): Int {
    return titleRes
  }

  companion object {
    @JvmStatic
    var ALL = arrayOf(UNREAD, PRIVATE_MESSAGES, COMMENT_REPLIES, POST_REPLIES, USERNAME_MENTIONS)
  }
}
