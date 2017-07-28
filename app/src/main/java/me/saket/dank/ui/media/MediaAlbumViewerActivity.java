package me.saket.dank.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.SystemUiHelper;
import me.saket.dank.utils.UrlParser;

public class MediaAlbumViewerActivity extends DankActivity
    implements MediaFragment.OnMediaItemClickListener, FlickGestureListener.GestureCallbacks, SystemUiHelper.OnSystemUiVisibilityChangeListener
{

  @BindView(R.id.mediaalbumviewer_root) ViewGroup rootLayout;
  @BindView(R.id.mediaalbumviewer_pager) ViewPager imagePager;

  private SystemUiHelper systemUiHelper;

  public static void start(Context context, MediaLink mediaLink) {
    context.startActivity(new Intent(context, MediaAlbumViewerActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_album_viewer);
    ButterKnife.bind(this);

    List<MediaAlbumItem> mediaLinks = new ArrayList<>();
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/WGDG7WH.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/psqmkjvtu0bz.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/5WT2RQF.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/u5dszwd3qjaz.jpg")));

    MediaAlbumPagerAdapter albumAdapter = new MediaAlbumPagerAdapter(getSupportFragmentManager(), mediaLinks);
    imagePager.setAdapter(albumAdapter);
  }

// ======== MEDIA FRAGMENT ======== //

  @Override
  public void onClickMediaItem() {
    if (systemUiHelper == null) {
      systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_LOW_PROFILE, 0 /* flags */, null /* listener */);
    }
    systemUiHelper.toggle();
  }

  @Override
  public void onPhotoFlick() {
    finish();
  }

  @Override
  public void onPhotoMove(@FloatRange(from = -1, to = 1) float moveRatio) {
    int dim = (int) (255 * (1f - Math.min(1f, Math.abs(moveRatio * 2))));
    if (rootLayout.getBackground() == null) {
      ColorDrawable backgroundDimDrawable = new ColorDrawable(Color.BLACK);
      rootLayout.setBackground(backgroundDimDrawable);
    }
    rootLayout.getBackground().setAlpha(dim);
  }

  @Override
  public void onSystemUiVisibilityChange(boolean systemUiVisible) {
  }
}
