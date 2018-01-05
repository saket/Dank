package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.data.PostedOrInFlightContribution;

public interface DraftStore {

  @CheckResult
  Completable saveDraft(PostedOrInFlightContribution contribution, String draft);

  @CheckResult
  Observable<String> streamDrafts(PostedOrInFlightContribution contribution);

  @CheckResult
  Completable removeDraft(PostedOrInFlightContribution contribution);
}
