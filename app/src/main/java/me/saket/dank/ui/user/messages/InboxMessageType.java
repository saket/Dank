package me.saket.dank.ui.user.messages;

import static junit.framework.Assert.assertEquals;

import net.dean.jraw.models.Message;

import me.saket.dank.BuildConfig;
import me.saket.dank.data.FullNameType;

public enum InboxMessageType {
  COMMENT_REPLY,
  USERNAME_MENTION,
  POST_REPLY,
  SUBREDDIT_MESSAGE,
  PRIVATE_MESSAGE,
  UNKNOWN;

  public static InboxMessageType parse(Message message) {
    if (!message.isComment()) {
      return InboxMessageType.PRIVATE_MESSAGE;
    }

    String parentFullName = message.getParentId();
    if (parentFullName == null) {
      return InboxMessageType.SUBREDDIT_MESSAGE;

    } else if (message.getSubject().equalsIgnoreCase("username mention")) {
      return USERNAME_MENTION;

    } else {
      FullNameType fullNameType = FullNameType.parse(parentFullName);
      switch (fullNameType) {
        case COMMENT:
          if (BuildConfig.DEBUG) {
            assertEquals("comment reply", message.getSubject());
          }
          return InboxMessageType.COMMENT_REPLY;

        case SUBMISSION:
          if (BuildConfig.DEBUG) {
            assertEquals("post reply", message.getSubject());
          }
          return InboxMessageType.POST_REPLY;

        case MESSAGE:
          throw new AssertionError("Shouldn't reach here.");

        default:
        case SUBREDDIT:
        case AWARD:
        case UNKNOWN:
          return InboxMessageType.UNKNOWN;
      }
    }
  }
}
