package me.saket.dank.utils;

import android.support.annotation.ColorRes;

import net.dean.jraw.models.VoteDirection;

import java.util.Collections;
import java.util.List;

import io.reactivex.functions.Function;
import me.saket.dank.R;

/**
 * Utility methods that don't fit in anywhere.
 */
public class Commons {

  public static <T> Function<List<T>, List<T>> toImmutable() {
    return list -> Collections.unmodifiableList(list);
  }

  @ColorRes
  public static int voteColor(VoteDirection voteDirection) {
    switch (voteDirection) {
      case UPVOTE:
        return R.color.vote_direction_upvote;
      case DOWNVOTE:
        return R.color.vote_direction_downvote;
      default:
      case NO_VOTE:
        return R.color.vote_direction_none;
    }
  }
}
