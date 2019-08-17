package me.saket.dank.ui.media;

import androidx.annotation.CheckResult;

import net.dean.jraw.models.SubmissionPreview;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.utils.Optional;

public interface MediaFragmentCallbacks {

  void toggleImmersiveMode();

  int getDeviceDisplayWidth();

  Single<Optional<SubmissionPreview>> getRedditSuppliedImages();

  @CheckResult
  Single<Integer> optionButtonsHeight();

  @CheckResult
  Observable<Boolean> systemUiVisibilityStream();
}
