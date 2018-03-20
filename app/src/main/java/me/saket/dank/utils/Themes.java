package me.saket.dank.utils;

import android.support.annotation.ColorRes;

import net.dean.jraw.models.VoteDirection;

import me.saket.dank.R;

public class Themes {

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
