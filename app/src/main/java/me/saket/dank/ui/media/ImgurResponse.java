package me.saket.dank.ui.media;

import java.util.List;

public interface ImgurResponse {

  String id();

  boolean hasImages();

  boolean isAlbum();

  List<ImgurImage> images();

  String albumTitle();

  String albumCoverImageUrl();
}
