package me.saket.dank.ui.submission;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static me.saket.dank.utils.Commons.toImmutable;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.ui.user.UserSession;
import timber.log.Timber;

/**
 * Manages sending replies and saving drafts.
 */
public class CommentsManager implements ReplyDraftStore {

  private final DankRedditClient dankRedditClient;
  private final BriteDatabase database;
  private final UserSession userSession;
  private final SharedPreferences sharedPreferences;
  private final Moshi moshi;

  public CommentsManager(DankRedditClient dankRedditClient, BriteDatabase database, UserSession userSession, SharedPreferences sharedPreferences,
      Moshi moshi)
  {
    this.dankRedditClient = dankRedditClient;
    this.database = database;
    this.userSession = userSession;
    this.sharedPreferences = sharedPreferences;
    this.moshi = moshi;
  }

// ======== REPLY ======== //

  /**
   * This exists to ensure a duplicate reply does not get stored when re-sending the same reply.
   */
  @CheckResult
  public Completable reSendReply(PendingSyncReply pendingSyncReply) {
    String parentSubmissionFullName = pendingSyncReply.parentSubmissionFullName();
    String parentContributionFullName = pendingSyncReply.parentContributionFullName();
    String replyBody = pendingSyncReply.body();
    long replyCreatedTimeMillis = pendingSyncReply.createdTimeMillis();
    return sendReply(parentSubmissionFullName, parentContributionFullName, replyBody, replyCreatedTimeMillis);
  }

  @CheckResult
  public Completable sendReply(PublicContribution parentContribution, String parentSubmissionFullName, String replyBody) {
    String parentContributionFullName = parentContribution.getFullName();
    long replyCreatedTimeMillis = System.currentTimeMillis();
    return sendReply(parentSubmissionFullName, parentContributionFullName, replyBody, replyCreatedTimeMillis);
  }

  @CheckResult
  private Completable sendReply(String parentSubmissionFullName, String parentContributionFullName, String replyBody, long replyCreatedTimeMillis) {
    PendingSyncReply pendingSyncReply = PendingSyncReply.create(
        replyBody,
        PendingSyncReply.State.POSTING,
        parentSubmissionFullName,
        parentContributionFullName,
        userSession.loggedInUserName(),
        replyCreatedTimeMillis
    );

    Timber.i("Sending reply to: %s", parentContributionFullName);

    ContributionFullNameWrapper fakeParentContribution = ContributionFullNameWrapper.create(parentContributionFullName);

    return Completable.fromAction(() -> database.insert(PendingSyncReply.TABLE_NAME, pendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE))
        .andThen(Single.fromCallable(() -> {
          String postedReplyId = dankRedditClient.userAccountManager().reply(fakeParentContribution, replyBody);
          return "t1_" + postedReplyId;   // full-name.
        }))
        .flatMapCompletable(postedReplyFullName -> Completable.fromAction(() -> {
          PendingSyncReply updatedPendingSyncReply = pendingSyncReply
              .toBuilder()
              .state(PendingSyncReply.State.POSTED)
              .postedFullName(postedReplyFullName)
              .build();
          database.insert(PendingSyncReply.TABLE_NAME, updatedPendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE);
        }))
        .onErrorResumeNext(error -> {
          Timber.e(error, "Couldn't send reply");

          PendingSyncReply updatedPendingSyncReply = pendingSyncReply.toBuilder()
              .state(PendingSyncReply.State.FAILED)
              .build();
          database.insert(PendingSyncReply.TABLE_NAME, updatedPendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE);
          return Completable.error(error);
        });
  }

  /**
   * Get all replies that are either awaiting to be posted or have been posted, but the comments
   * haven't been refreshed for <var>submission</var> yet.
   */
  @CheckResult
  public Observable<List<PendingSyncReply>> streamPendingSyncRepliesForSubmission(Submission submission) {
    return toV2Observable(
        database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_FOR_SUBMISSION, submission.getFullName())
            .mapToList(PendingSyncReply.MAPPER))
        .map(toImmutable());
  }

  @CheckResult
  public Observable<List<PendingSyncReply>> streamFailedReplies() {
    return toV2Observable(database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_FAILED).mapToList(PendingSyncReply.MAPPER))
        .map(toImmutable());
  }

  @CheckResult
  private Observable<List<PendingSyncReply>> streamPendingSyncPostedRepliesForSubmission(Submission submission) {
    return toV2Observable(
        database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_POSTED_FOR_SUBMISSION, submission.getFullName())
            .mapToList(PendingSyncReply.MAPPER))
        .map(toImmutable());
  }

  /**
   * Removes "pending-sync" replies for a submission once they're found in the submission's comments.
   */
  @CheckResult
  public Completable removeSyncPendingPostedRepliesForSubmission(Submission submission) {
    return streamPendingSyncPostedRepliesForSubmission(submission)
        .firstOrError()
        .map(pendingSyncReplies -> {
          Map<String, PendingSyncReply> postedFullNameToPendingSyncReplyMap = new HashMap<>(pendingSyncReplies.size(), 1);
          for (PendingSyncReply pendingSyncReply : pendingSyncReplies) {
            postedFullNameToPendingSyncReplyMap.put(pendingSyncReply.postedFullName(), pendingSyncReply);
          }

          List<PendingSyncReply> pendingSyncRepliesToRemove = new ArrayList<>();

          for (CommentNode commentNode : submission.getComments().walkTree()) {
            String commentFullName = commentNode.getComment().getFullName();
            if (postedFullNameToPendingSyncReplyMap.containsKey(commentFullName)) {
              pendingSyncRepliesToRemove.add(postedFullNameToPendingSyncReplyMap.get(commentFullName));
            }
          }

          return pendingSyncRepliesToRemove;
        })
        .flatMapCompletable(pendingSyncRepliesToRemove -> Completable.fromAction(() -> {
          try (BriteDatabase.Transaction transaction = database.newTransaction()) {
            for (PendingSyncReply replyToRemove : pendingSyncRepliesToRemove) {
              database.delete(
                  PendingSyncReply.TABLE_NAME,
                  PendingSyncReply.WHERE_BODY_AND_CREATED_TIME_2,
                  replyToRemove.body(),
                  String.valueOf(replyToRemove.createdTimeMillis())
              );
            }
            transaction.markSuccessful();
          }
        }));
  }

  @CheckResult
  public Completable removeAllPendingSyncReplies() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }
    return Completable.fromAction(() -> database.delete(PendingSyncReply.TABLE_NAME, null));
  }

// ======== DRAFTS ======== //

  @Override
  @CheckResult
  public Completable saveDraft(PublicContribution parentContribution, String draftBody) {
    return Completable.fromAction(() -> {
      long draftCreatedTimeMillis = System.currentTimeMillis();
      ReplyDraft replyDraft = ReplyDraft.create(draftBody, draftCreatedTimeMillis);

      JsonAdapter<ReplyDraft> jsonAdapter = moshi.adapter(ReplyDraft.class);
      String replyDraftJson = jsonAdapter.toJson(replyDraft);
      sharedPreferences.edit().putString(keyForDraft(parentContribution), replyDraftJson).apply();

      // TODO:
      // Recycle old drafts.
    });
  }

  @Override
  @CheckResult
  public Single<String> getDraft(PublicContribution parentContribution) {
    return Single.fromCallable(() -> {
      String replyDraftJson = sharedPreferences.getString(keyForDraft(parentContribution), "");
      if ("".equals(replyDraftJson)) {
        return "";
      }

      ReplyDraft replyDraft = moshi.adapter(ReplyDraft.class).fromJson(replyDraftJson);
      return replyDraft.body();
    });
  }

  private static String keyForDraft(PublicContribution parentContribution) {
    return "replyDraftFor_" + parentContribution.getFullName();
  }
}
