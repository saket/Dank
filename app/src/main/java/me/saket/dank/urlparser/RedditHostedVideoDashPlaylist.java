package me.saket.dank.urlparser;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditHostedVideoDashPlaylist {

  public abstract String dashUrl();

  public abstract String directUrlWithoutAudio();

  public static RedditHostedVideoDashPlaylist create(String playlistUrl, String dashUrlWithoutAudio) {
    return new AutoValue_RedditHostedVideoDashPlaylist(playlistUrl, dashUrlWithoutAudio);
  }
}
