package me.saket.dank.ui.submission;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import me.saket.dank.ui.UiChange;
import me.saket.dank.ui.UiEvent;

public class SubmissionController implements ObservableTransformer<UiEvent, UiChange<SubmissionUi>> {

  @Override
  public ObservableSource<UiChange<SubmissionUi>> apply(Observable<UiEvent> upstream) {
    return null;
  }
}
