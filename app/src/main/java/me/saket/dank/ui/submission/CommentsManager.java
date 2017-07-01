package me.saket.dank.ui.submission;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.Strings;
import timber.log.Timber;

/**
 * Manages sending replies and saving drafts.
 */
public class CommentsManager {

  private final DankRedditClient dankRedditClient;
  private final BriteDatabase database;
  private final UserSession userSession;

  // TODO: Remove this.
  private final Map<String, String> inMemoryDraftMap = new HashMap<>();

  public CommentsManager(DankRedditClient dankRedditClient, BriteDatabase database, UserSession userSession) {
    this.dankRedditClient = dankRedditClient;
    this.database = database;
    this.userSession = userSession;
  }

  @CheckResult
  public Completable sendReply(CommentNode parentCommentNode, String reply) {
    String parentSubmissionFullName = parentCommentNode.getSubmissionName();
    Comment parentComment = parentCommentNode.getComment();
    long replyCreatedTime = System.currentTimeMillis();

    PendingSyncReply pendingSyncReply = PendingSyncReply.create(
        parentComment.getFullName(),
        reply,
        PendingSyncReply.State.POSTING,
        parentSubmissionFullName,
        userSession.loggedInUserName(),
        replyCreatedTime
    );

    return Completable.fromAction(() -> database.insert(PendingSyncReply.TABLE_NAME, pendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE))
        .andThen(Completable.fromAction(() -> dankRedditClient.userAccountManager().reply(parentComment, reply)))
        .andThen(Completable.fromAction(() -> {
          PendingSyncReply updatedPendingSyncReply = pendingSyncReply.withType(PendingSyncReply.State.POSTED);
          database.insert(PendingSyncReply.TABLE_NAME, updatedPendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE);
        }))
        .onErrorResumeNext(error -> {
          PendingSyncReply updatedPendingSyncReply = pendingSyncReply.withType(PendingSyncReply.State.FAILED);
          database.insert(PendingSyncReply.TABLE_NAME, updatedPendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE);
          Timber.e(error, "Couldn't post reply");
          return Completable.complete();
        });
  }

  /**
   * Get all replies that are either awaiting to be posted or have been posted, but the comments
   * haven't been refreshed for <var>submission</var> yet.
   */
  @CheckResult
  public Observable<List<PendingSyncReply>> pendingSncRepliesForSubmission(Submission submission) {
    return toV2Observable(
        database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_FOR_SUBMISSION, submission.getFullName())
            .mapToList(PendingSyncReply.MAPPER));
  }

  /**
   * Removes "pending-sync" replies for a submission once they're found in the submission's comments.
   */
  @CheckResult
  public Completable removeSyncPendingPostedRepliesForSubmission(Submission submission) {
    return pendingSncRepliesForSubmission(submission)
        .firstOrError()
        .map(pendingSyncReplies -> {
          Map<String, PendingSyncReply> parentFullNameToPendingSyncReplyMap = new HashMap<>(pendingSyncReplies.size(), 1);
          for (PendingSyncReply pendingSyncReply : pendingSyncReplies) {
            parentFullNameToPendingSyncReplyMap.put(pendingSyncReply.parentCommentFullName(), pendingSyncReply);
          }

          List<PendingSyncReply> pendingSyncRepliesToRemove = new ArrayList<>();

          for (CommentNode commentNode : submission.getComments().walkTree()) {
            String commentFullName = commentNode.getComment().getFullName();
            if (parentFullNameToPendingSyncReplyMap.containsKey(commentFullName)) {
              pendingSyncRepliesToRemove.add(parentFullNameToPendingSyncReplyMap.get(commentFullName));
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
  public Completable saveDraft(Comment parentComment, String draft) {
    return Completable.fromAction(() -> inMemoryDraftMap.put(parentComment.getFullName(), draft));
  }

  @CheckResult
  public Single<String> getDraft(Comment parentComment) {
    return Single.just(inMemoryDraftMap.get(parentComment.getFullName()));
  }

  @CheckResult
  public Completable removeAll() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }
    return Completable.fromAction(() -> database.delete(PendingSyncReply.TABLE_NAME, null));
  }
}
