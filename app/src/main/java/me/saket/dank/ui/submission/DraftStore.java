package me.saket.dank.ui.submission;

import androidx.annotation.CheckResult;

import net.dean.jraw.models.Identifiable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.reply.DraftSaveResult;

public interface DraftStore {

  @CheckResult
  Single<DraftSaveResult> saveDraft(Identifiable identifiable, String draftBody);

  @CheckResult
  Observable<String> streamDrafts(Identifiable identifiable);

  @CheckResult
  Completable removeDraft(Identifiable identifiable);
}
