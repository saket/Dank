package me.saket.dank.ui.compose;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.ArrayRes;
import androidx.annotation.CheckResult;

import com.jakewharton.rxbinding2.widget.RxPopupMenu;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class EmojiPopup extends PopupMenu {

  private Observable<String> emojiSelectStream = PublishSubject.create();

  public EmojiPopup(Context context, View anchor, @ArrayRes int emojiArrayRes) {
    super(context, anchor, Gravity.TOP);

    String[] unicodeEmojis = context.getResources().getStringArray(emojiArrayRes);
    for (String unicodeEmoji : unicodeEmojis) {
      getMenu().add(unicodeEmoji);
    }

    emojiSelectStream = RxPopupMenu.itemClicks(this)
        .map(item -> item.getTitle().toString())
        .doOnNext(emoji -> Timber.i("Sending: %s", emoji))
        .takeUntil(RxPopupMenu.dismisses(this));
  }

  @CheckResult
  public Observable<String> streamEmojiSelections() {
    return emojiSelectStream;
  }
}
