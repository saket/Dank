package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Contribution;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface ReplyDraftStore {

  @CheckResult
  Completable saveDraft(Contribution contribution, String draft);

  @CheckResult
  Single<String> getDraft(Contribution contribution);

  @CheckResult
  Completable removeDraft(Contribution contribution);
}
