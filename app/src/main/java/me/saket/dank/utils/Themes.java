package me.saket.dank.utils;

import androidx.annotation.ColorRes;

import net.dean.jraw.models.VoteDirection;

import me.saket.dank.R;

public class Themes {

  @ColorRes
  public static int voteColor(VoteDirection voteDirection) {
    switch (voteDirection) {
      case UP:
        return R.color.vote_direction_upvote;
      case DOWN:
        return R.color.vote_direction_downvote;
      default:
      case NONE:
        return R.color.vote_direction_none;
    }
  }
}
