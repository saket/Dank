package me.saket.dank.ui.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.media.MediaAlbumAdapter.AlbumImageViewHolder;
import me.saket.dank.utils.UrlParser;

public class MediaAlbumViewerActivity extends DankActivity {

  @BindView(R.id.mediaalbumviewer_list) RecyclerView albumMediaList;

  public static void start(Context context, MediaLink mediaLink) {
    context.startActivity(new Intent(context, MediaAlbumViewerActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_album_viewer);
    ButterKnife.bind(this);

    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
    albumMediaList.setLayoutManager(linearLayoutManager);

    SnapHelper snapHelper = new PagerSnapHelper();
    snapHelper.attachToRecyclerView(albumMediaList);

    List<MediaAlbumItem> mediaLinks = new ArrayList<>();
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/WGDG7WH.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/psqmkjvtu0bz.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/5WT2RQF.jpg")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/u5dszwd3qjaz.jpg")));

    int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    MediaAlbumAdapter albumAdapter = new MediaAlbumAdapter(deviceDisplayWidth);
    albumAdapter.updateDataAndNotifyDatasetChanged(mediaLinks);
    albumMediaList.setAdapter(albumAdapter);

    albumMediaList.addOnItemTouchListener(createTouchListenerForImagePanning());
  }

  @NonNull
  private RecyclerView.OnItemTouchListener createTouchListenerForImagePanning() {
    return new RecyclerView.OnItemTouchListener() {
      private PointF onActionDownTouch = new PointF();

      @Override
      public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            onActionDownTouch.set(event.getX(), event.getY());
            break;

          case MotionEvent.ACTION_MOVE:
            // We're not comparing the delta with touchSlop because RV seems to be ignoring
            // requestDisallowInterceptTouchEvent() calls if it's made after passing the touch slop.
            float deltaX = event.getX() - onActionDownTouch.x;
            float deltaY = event.getY() - onActionDownTouch.y;
            float absDeltaX = Math.abs(deltaX);
            float absDeltaY = Math.abs(deltaY);

            boolean isSwipingVertically = absDeltaX < absDeltaY;

            View viewUnderTouch = rv.findChildViewUnder(event.getX(), event.getY());
            if (viewUnderTouch != null) {
              AlbumImageViewHolder viewHolderUnderTouch = (AlbumImageViewHolder) rv.findContainingViewHolder(viewUnderTouch);
              if (viewHolderUnderTouch == null) {
                throw new NullPointerException();
              }

              boolean canPanFurther;
              if (isSwipingVertically) {
                boolean isPanningUpwards = deltaY < 0;
                canPanFurther = viewHolderUnderTouch.imageView.canPanVertically(isPanningUpwards);

              } else {
                boolean isPanningTowardsRight = deltaX > 0;
                canPanFurther = viewHolderUnderTouch.imageView.canPanHorizontally(isPanningTowardsRight);
              }

              if (canPanFurther) {
                albumMediaList.requestDisallowInterceptTouchEvent(true);
              }
            }
            break;
        }
        return false;
      }

      @Override
      public void onTouchEvent(RecyclerView rv, MotionEvent e) {}

      @Override
      public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    };
  }
}
