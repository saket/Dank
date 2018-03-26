package me.saket.dank.urlparser;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * Reddit uses DASH for its videos, which automatically switches between video qualities
 * depending upon the user's network connection, which is fantastic!
 */
@AutoValue
public abstract class RedditHostedVideoLink extends MediaLink implements Parcelable {

  @Override
  public Type type() {
    return Type.SINGLE_VIDEO;
  }

  @Override
  public abstract String unparsedUrl();

  @Override
  public abstract String highQualityUrl();

  @Override
  public String lowQualityUrl() {
    return highQualityUrl();
  }

  public abstract String directUrlWithoutAudio();

  public static Link create(String unparsedUrl, RedditHostedVideoDashPlaylist playlist) {
    String dashPlaylistUrl = playlist.dashUrl();
    String directVideoUrlWithoutAudio = playlist.directUrlWithoutAudio();
    return new AutoValue_RedditHostedVideoLink(unparsedUrl, dashPlaylistUrl, directVideoUrlWithoutAudio);
  }
}
