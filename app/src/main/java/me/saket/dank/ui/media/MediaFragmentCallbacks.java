package me.saket.dank.ui.media;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;

import net.dean.jraw.models.Thumbnails;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface MediaFragmentCallbacks {
  void onClickMediaItem();

  int getDeviceDisplayWidth();

  @Nullable
  Thumbnails getRedditSuppliedImages();

  @CheckResult
  Single<Integer> optionButtonsHeight();

  @CheckResult
  Observable<Boolean> systemUiVisibilityStream();

  LinkMovementMethod getMediaDescriptionLinkMovementMethod();
}
