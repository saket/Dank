package me.saket.dank.ui.media;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Thumbnails;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.utils.Optional;

public interface MediaFragmentCallbacks {

  void toggleImmersiveMode();

  int getDeviceDisplayWidth();

  Optional<Thumbnails> getRedditSuppliedImages();

  @CheckResult
  Single<Integer> optionButtonsHeight();

  @CheckResult
  Observable<Boolean> systemUiVisibilityStream();
}
