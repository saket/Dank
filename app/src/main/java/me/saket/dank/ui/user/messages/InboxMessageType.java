package me.saket.dank.ui.user.messages;

import static junit.framework.Assert.assertEquals;

import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import me.saket.dank.data.FullNameType;

public enum InboxMessageType {
  COMMENT_REPLY,
  POST_REPLY,
  SUBREDDIT_MESSAGE,
  PRIVATE_MESSAGE,
  UNKNOWN;

  public static InboxMessageType parse(Message message) {
    String parentFullName = message.getParentId();
    if (parentFullName == null) {
      return InboxMessageType.SUBREDDIT_MESSAGE;

    } else {
      FullNameType fullNameType = FullNameType.parse(parentFullName);
      switch (fullNameType) {
        case COMMENT:
          assertEquals("comment reply", message.getSubject());
          return InboxMessageType.COMMENT_REPLY;

        case SUBMISSION:
          assertEquals("post reply", message.getSubject());
          return InboxMessageType.POST_REPLY;

        case MESSAGE:
          assertEquals(true, message instanceof PrivateMessage);
          return InboxMessageType.PRIVATE_MESSAGE;

        default:
        case SUBREDDIT:
        case AWARD:
        case UNKNOWN:
          return InboxMessageType.UNKNOWN;
      }
    }
  }
}
