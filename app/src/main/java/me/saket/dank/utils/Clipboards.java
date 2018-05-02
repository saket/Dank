package me.saket.dank.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class Clipboards {

  public static void save(Context c, String plainText) {
    ClipboardManager clipboardManager = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
    //noinspection ConstantConditions
    clipboardManager.setPrimaryClip(ClipData.newPlainText(plainText, plainText));
  }

  public static Optional<String> currentPlainText(Context context) {
    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    assert clipboard != null;

    return Optional.ofNullable(clipboard.getPrimaryClip())
        .map(clip -> clip.getItemAt(0))
        .map(item -> item.coerceToText(context))
        .map(CharSequence::toString);
  }
}
