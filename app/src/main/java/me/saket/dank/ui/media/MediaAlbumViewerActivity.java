package me.saket.dank.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.SystemUiHelper;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

public class MediaAlbumViewerActivity extends DankActivity
    implements MediaFragment.OnMediaItemClickListener, FlickGestureListener.GestureCallbacks, SystemUiHelper.OnSystemUiVisibilityChangeListener
{

  @BindView(R.id.mediaalbumviewer_root) ViewGroup rootLayout;
  @BindView(R.id.mediaalbumviewer_pager) ViewPagerWithManualScrollBlock mediaPager;

  private SystemUiHelper systemUiHelper;
  private Drawable activityBackgroundDrawable;

  public static void start(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaAlbumViewerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_album_viewer);
    ButterKnife.bind(this);

    List<MediaAlbumItem> mediaLinks = new ArrayList<>();
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/WGDG7WH.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/psqmkjvtu0bz.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/5WT2RQF.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/u5dszwd3qjaz.jpg")));

    MediaAlbumPagerAdapter albumAdapter = new MediaAlbumPagerAdapter(getSupportFragmentManager(), mediaLinks);
    mediaPager.setAdapter(albumAdapter);
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, 0);
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
  public void onFlickDismiss() {
    finish();
  }

  @Override
  public void onMoveMedia(@FloatRange(from = -1, to = 1) float moveRatio) {
    int dim = (int) (255 * (1f - Math.min(1f, Math.abs(moveRatio * 2))));
    if (activityBackgroundDrawable == null) {
      activityBackgroundDrawable = rootLayout.getBackground().mutate();
      rootLayout.setBackground(activityBackgroundDrawable);
    }
    activityBackgroundDrawable.setAlpha(dim);
  }

  @Override
  public void setViewPagerScrollingBlocked(boolean blocked) {
    mediaPager.setScrollingBlocked(blocked);
  }

  @Override
  public void onSystemUiVisibilityChange(boolean systemUiVisible) {
  }
}
