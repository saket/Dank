package me.saket.dank.data;

import java.util.List;

public interface ImgurResponse {

  boolean hasImages();

  boolean isAlbum();

  List<ImgurImage> images();

  String albumTitle();

}
