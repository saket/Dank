package me.saket.dank.ui.media;

import java.util.List;

public interface ImgurResponse {

  boolean hasImages();

  boolean isAlbum();

  List<ImgurImage> images();

  String albumTitle();

  String albumCoverImageUrl();
}
