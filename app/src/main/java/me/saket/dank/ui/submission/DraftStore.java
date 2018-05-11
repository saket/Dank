package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Contribution;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.reply.DraftSaveResult;

public interface DraftStore {

  @CheckResult
  Single<DraftSaveResult> saveDraft(Contribution contribution, String draftBody);

  @CheckResult
  Observable<String> streamDrafts(Contribution contribution);

  @CheckResult
  Completable removeDraft(Contribution contribution);
}
