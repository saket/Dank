package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Comment;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface ReplyDraftStore {

  @CheckResult
  Completable saveDraft(Comment parentComment, String draft);

  @CheckResult
  Single<String> getDraft(Comment parentComment);
}
