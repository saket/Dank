package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Contribution;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface DraftStore {

  @CheckResult
  Completable saveDraft(Contribution contribution, String draft);

  @CheckResult
  Observable<String> streamDrafts(Contribution contribution);

  @CheckResult
  Completable removeDraft(Contribution contribution);
}
