package me.saket.dank.utils;

import android.support.annotation.StringRes;

import net.dean.jraw.models.CommentSort;

import me.saket.dank.R;

// TODO Kotlin: Convert to extension functions.
public class CommentSortUtils {

  @StringRes
  public static int sortingDisplayTextRes(CommentSort sort) {
    switch (sort) {
      case CONFIDENCE:
        return R.string.comment_sorting_best;

      case TOP:
        return R.string.comment_sorting_top;

      case NEW:
        return R.string.comment_sorting_new;

      case HOT:
        throw new AssertionError("HOT comment sort isn't supported anymore");

      case CONTROVERSIAL:
        return R.string.comment_sorting_controversial;

      case OLD:
        return R.string.comment_sorting_old;

      case QA:
        return R.string.comment_sorting_qna;

      default:
        throw new UnsupportedOperationException("Unknown comment sort: " + sort);
    }
  }
}
