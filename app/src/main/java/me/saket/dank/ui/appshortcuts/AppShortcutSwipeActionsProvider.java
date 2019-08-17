package me.saket.dank.ui.appshortcuts;

import androidx.annotation.StringRes;

import com.jakewharton.rxrelay2.PublishRelay;

import javax.inject.Inject;

import me.saket.dank.R;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;

/**
 * Controls gesture actions on {@link AppShortcut}.
 */
public class AppShortcutSwipeActionsProvider {

  private static final @StringRes int ACTION_NAME_DELETE = R.string.appshrotcuts_swipe_action_delete;

  private final SwipeActions swipeActions;
  private final SwipeActionIconProvider swipeActionIconProvider;
  public final PublishRelay<AppShortcut> deleteSwipeActions = PublishRelay.create();

  @Inject
  public AppShortcutSwipeActionsProvider() {
    SwipeAction deleteAction = SwipeAction.create(ACTION_NAME_DELETE, R.color.appshortcut_swipe_delete, 1);
    swipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(deleteAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(deleteAction)
            .build())
        .build();

    swipeActionIconProvider = createActionIconProvider();
  }

  public SwipeActions actions() {
    return swipeActions;
  }

  public SwipeActionIconProvider iconProvider() {
    return swipeActionIconProvider;
  }

  public SwipeActionIconProvider createActionIconProvider() {
    return (imageView, oldAction, newAction) -> {
      if (newAction.labelRes() == ACTION_NAME_DELETE) {
        imageView.setImageResource(R.drawable.ic_delete_20dp);

      } else {
        throw new AssertionError("Unknown swipe action: " + newAction);
      }
    };
  }

  public void performSwipeAction(SwipeAction swipeAction, AppShortcut shortcut, SwipeableLayout swipeableLayout, SwipeDirection swipeDirection) {
    switch (swipeAction.labelRes()) {
      case ACTION_NAME_DELETE:
        deleteSwipeActions.accept(shortcut);
        break;

      default:
        throw new AssertionError("Unknown swipe action: " + swipeAction);
    }
    swipeableLayout.playRippleAnimation(swipeAction, RippleType.REGISTER, swipeDirection);
  }
}
