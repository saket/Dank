package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.PublicContribution;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface ReplyDraftStore {

  @CheckResult
  Completable saveDraft(PublicContribution parentContribution, String draft);

  @CheckResult
  Single<String> getDraft(PublicContribution parentContribution);
}
