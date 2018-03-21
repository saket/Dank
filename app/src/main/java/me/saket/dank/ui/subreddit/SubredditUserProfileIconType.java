package me.saket.dank.ui.subreddit;

import me.saket.dank.R;

public enum SubredditUserProfileIconType {
  USER_PROFILE {
    @Override
    public int iconRes() {
      return R.drawable.ic_user_profile_24dp;
    }
  },

  USER_PROFILE_WITH_UNREAD_MESSAGES {
    @Override
    public int iconRes() {
      return R.drawable.ic_mail_unread_24dp;
    }
  };

  public int iconRes() {
    throw new AbstractMethodError();
  }
}
