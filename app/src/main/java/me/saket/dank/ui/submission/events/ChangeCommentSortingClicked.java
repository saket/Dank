package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentSort;

import io.reactivex.Single;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.AuditedCommentSort;
import me.saket.dank.ui.submission.CommentSortingModePopupMenu;

@AutoValue
public abstract class ChangeCommentSortingClicked implements UiEvent {

  public abstract View buttonView();

  public static ChangeCommentSortingClicked create(View buttonView) {
    return new AutoValue_ChangeCommentSortingClicked(buttonView);
  }
}
