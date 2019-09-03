package me.saket.dank.notifs;

import net.dean.jraw.models.Message;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.notifs.MessagesNotificationManager.SeenUnreadMessagesIdStore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagesNotificationManagerTest {

  private MessagesNotificationManager notificationManager;

  @Before
  public void setUp() throws Exception {
    //noinspection ConstantConditions
    SeenUnreadMessagesIdStore seenUnreadMessagesIdStore = new SeenUnreadMessagesIdStore(null) {
      private Set<String> seenMessageIds = Collections.emptySet();

      @Override
      public Completable save(Set<String> seenMessageIds) {
        return Completable.fromAction(() -> this.seenMessageIds = seenMessageIds);
      }

      @Override
      public Single<Set<String>> get() {
        return Single.just(seenMessageIds);
      }
    };

    //noinspection ConstantConditions
    notificationManager = new MessagesNotificationManager(seenUnreadMessagesIdStore, null, null, null);
  }

  @Test
  public void whenMultipleUnreadsAreFetched_andMultipleMessagesAreMarkedAsRead_shouldFilterSeenMessagesCorrectly() {
    // New unreads received from server.
    List<Message> unreadMessages = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Message mockedMessage = mock(Message.class);
      when(mockedMessage.getId()).thenReturn(String.valueOf(i));
      when(mockedMessage.getBody()).thenReturn(String.valueOf(i));
      unreadMessages.add(mockedMessage);
    }

    // Notifs are generated.
    // Condition: Nothing should be filtered.
    List<Message> filteredUnseenMessages = notificationManager
        .filterUnseenMessages(unreadMessages)
        .blockingGet();
    assertEquals(filteredUnseenMessages.size(), unreadMessages.size());

    // User marks the first message as read.
    // Notifs are refreshed.
    notificationManager
        .markMessageNotifAsSeen(unreadMessages.get(0))
        .andThen(notificationManager.removeMessageNotifSeenStatus(unreadMessages.get(0)))
        .subscribe();

    // Notifs are refreshed after marking the first as read.
    // Condition: should not contain the message marked as read.
    filteredUnseenMessages = notificationManager
        .filterUnseenMessages(unreadMessages)
        .blockingGet();
    assertEquals(filteredUnseenMessages.size(), unreadMessages.size() - 1);
    assertEquals(filteredUnseenMessages.contains(unreadMessages.get(0)), false);

    // More unreads are fetched.
    for (int i = 3; i < 5; i++) {
      Message mockedMessage = mock(Message.class);
      when(mockedMessage.getId()).thenReturn(String.valueOf(i));
      when(mockedMessage.getBody()).thenReturn(String.valueOf(i));
      unreadMessages.add(mockedMessage);
    }

    // Notifs are refreshed.
    // Condition: Should only filter one message.
    filteredUnseenMessages = notificationManager
        .filterUnseenMessages(unreadMessages)
        .blockingGet();
    assertEquals(filteredUnseenMessages.size(), unreadMessages.size() - 1);
    assertEquals(filteredUnseenMessages.contains(unreadMessages.get(0)), false);

    // User marks another message as read.
    notificationManager
        .markMessageNotifAsSeen(unreadMessages.get(1))
        .andThen(notificationManager.removeMessageNotifSeenStatus(unreadMessages.get(1)))
        .subscribe();

    // Notifs are refreshed.
    // Condition: Should filter the two marked-as-read messages.
    filteredUnseenMessages = notificationManager
        .filterUnseenMessages(unreadMessages)
        .blockingGet();
    assertEquals(filteredUnseenMessages.size(), unreadMessages.size() - 2);
    assertEquals(filteredUnseenMessages.contains(unreadMessages.get(0)), false);
    assertEquals(filteredUnseenMessages.contains(unreadMessages.get(1)), false);

    // All notifs are cleared.
    List<String> messageIdsToMarkAsSeen = new ArrayList<>();
    for (Message unreadMessage : unreadMessages) {
      messageIdsToMarkAsSeen.add(unreadMessage.getId());
    }
    notificationManager
        .markMessageNotifAsSeen(messageIdsToMarkAsSeen)
        .subscribe();

    // Notifs are refreshed.
    filteredUnseenMessages = notificationManager
        .filterUnseenMessages(unreadMessages)
        .blockingGet();
    assertEquals(filteredUnseenMessages.size(), 0);
  }
}
