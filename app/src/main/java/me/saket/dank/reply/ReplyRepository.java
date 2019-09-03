package me.saket.dank.reply;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.CheckResult;
import androidx.annotation.VisibleForTesting;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.Identifiable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Lazy;
import hirondelle.date4j.DateTime;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.FullNameType;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.compose.SimpleIdentifiable;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.Preconditions;
import timber.log.Timber;

/**
 * Manages sending replies and saving drafts.
 */
@Singleton
public class ReplyRepository implements DraftStore {

  private final Lazy<Reddit> reddit;
  private final BriteDatabase database;
  private final UserSessionRepository userSessionRepository;
  private final Moshi moshi;
  private final int recycleDraftsOlderThanNumDays;
  private final SharedPreferences sharedPrefs;
  private final RxSharedPreferences rxSharedPrefs;
  private final Lazy<ErrorResolver> errorResolver;

  @Inject
  public ReplyRepository(
      Lazy<Reddit> reddit,
      BriteDatabase database,
      UserSessionRepository userSessionRepository,
      @Named("drafts") SharedPreferences sharedPrefs,
      Moshi moshi,
      @Named("drafts_max_retain_days") int recycleDraftsOlderThanNumDays,
      Lazy<ErrorResolver> errorResolver)
  {
    this.reddit = reddit;
    this.database = database;
    this.userSessionRepository = userSessionRepository;
    this.sharedPrefs = sharedPrefs;
    this.rxSharedPrefs = RxSharedPreferences.create(sharedPrefs);
    this.moshi = moshi;
    this.recycleDraftsOlderThanNumDays = recycleDraftsOlderThanNumDays;
    this.errorResolver = errorResolver;
  }

// ======== INLINE_REPLY ======== //

  /**
   * This exists to ensure a duplicate reply does not get stored when re-sending the same reply.
   */
  @CheckResult
  public Completable reSendReply(PendingSyncReply pendingSyncReply) {
    String parentThreadFullName = pendingSyncReply.parentThreadFullName();
    ParentThread parentThread;
    if (parentThreadFullName.startsWith(FullNameType.SUBMISSION.prefix())) {
      parentThread = ParentThread.createSubmission(parentThreadFullName);
    } else if (parentThreadFullName.startsWith(FullNameType.MESSAGE.prefix())) {
      parentThread = ParentThread.createPrivateMessage(parentThreadFullName);
    } else {
      throw new UnsupportedOperationException("Unknown thread name: " + parentThreadFullName);
    }

    String replyBody = pendingSyncReply.body();
    long replyCreatedTimeMillis = pendingSyncReply.createdTimeMillis();
    Identifiable parentContribution = SimpleIdentifiable.Companion.from(pendingSyncReply.parentContributionFullName());
    return sendReply(parentContribution, parentThread, replyBody, replyCreatedTimeMillis);
  }

  /**
   * @param parentContribution Parent comment/message.
   * @param parentThread       Root submission/message-thread.
   */
  @CheckResult
  public Completable sendReply(Identifiable parentContribution, ParentThread parentThread, String replyBody) {
    // Converts ¯\_(ツ)_/¯ to ¯\\_(ツ)_/¯.
    String replyWithSlashesEscaped = replyBody.replaceAll(Matcher.quoteReplacement("\\"), Matcher.quoteReplacement("\\\\"));

    long replyCreatedTimeMillis = System.currentTimeMillis();
    return sendReply(Reply.create(parentContribution, parentThread, replyWithSlashesEscaped, replyCreatedTimeMillis));
  }

  @CheckResult
  private Completable sendReply(Identifiable parentContribution, ParentThread parentThread, String replyBody, long replyCreatedTimeMillis) {
    return sendReply(Reply.create(parentContribution, parentThread, replyBody, replyCreatedTimeMillis));
  }

  Completable sendReply(Reply reply) {
    long sentTimeMillis = System.currentTimeMillis();
    PendingSyncReply pendingSyncReply = reply.toPendingSync(userSessionRepository, sentTimeMillis);

    return Completable.fromAction(() -> database.insert(PendingSyncReply.TABLE_NAME, pendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE))
        .andThen(reply.sendToRemote(reddit.get()))
        .flatMapCompletable(postedReply -> Completable.fromAction(() -> {
          PendingSyncReply updatedPendingSyncReply = pendingSyncReply
              .toBuilder()
              .state(PendingSyncReply.State.POSTED)
              .postedFullName(postedReply.getFullName())
              .build();
          database.insert(PendingSyncReply.TABLE_NAME, updatedPendingSyncReply.toValues(), SQLiteDatabase.CONFLICT_REPLACE);
        }))
        .onErrorResumeNext(error -> {
          ResolvedError resolvedError = errorResolver.get().resolve(error);
          resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't send reply"));

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
  public Observable<List<PendingSyncReply>> streamPendingSyncReplies(ParentThread parentThread) {
    return database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_FOR_THREAD, parentThread.fullName())
        .mapToList(PendingSyncReply.MAPPER)
        .as(Arrays2.immutable());
  }

  @CheckResult
  public Observable<List<PendingSyncReply>> streamFailedReplies() {
    return database.createQuery(PendingSyncReply.TABLE_NAME, PendingSyncReply.QUERY_GET_ALL_FAILED)
        .mapToList(PendingSyncReply.MAPPER)
        .as(Arrays2.immutable());
  }

  /**
   * Removes POSTED "pending-sync" replies for a submission/message-thread once its comments are refreshed.
   */
  @CheckResult
  public Completable removeSyncPendingPostedReplies(ParentThread parentThread) {
    return Completable.fromAction(() -> database.delete(
        PendingSyncReply.TABLE_NAME,
        PendingSyncReply.WHERE_STATE_AND_THREAD_FULL_NAME,
        PendingSyncReply.State.POSTED.name(), parentThread.fullName()
    ));
  }

  @CheckResult
  public Completable removeAllPendingSyncReplies() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }
    return Completable.fromAction(() -> database.delete(PendingSyncReply.TABLE_NAME, null));
  }

// ======== DRAFTS ======== //

  /**
   * @return True if the draft was saved. False otherwise.
   */
  @Override
  public Single<DraftSaveResult> saveDraft(Identifiable identifiable, String draftBody) {
    if (draftBody.isEmpty()) {
      return removeDraft(identifiable).toSingleDefault(DraftSaveResult.REMOVED);
    }

    return Completable
        .fromAction(() -> {
          long draftCreatedTimeMillis = System.currentTimeMillis();
          ReplyDraft replyDraft = ReplyDraft.create(draftBody, draftCreatedTimeMillis);
          JsonAdapter<ReplyDraft> jsonAdapter = moshi.adapter(ReplyDraft.class);
          String replyDraftJson = jsonAdapter.toJson(replyDraft);
          rxSharedPrefs.getString(keyForDraft(identifiable)).set(replyDraftJson);
          //Timber.i("Draft saved: %s", draftBody);

          // Recycle old drafts.
          Map<String, ?> allDraftJsons = new HashMap<>(sharedPrefs.getAll());
          Map<String, ReplyDraft> allDrafts = new HashMap<>(allDraftJsons.size());
          for (Map.Entry<String, ?> entry : allDraftJsons.entrySet()) {
            String draftEntryJson = (String) entry.getValue();
            //Timber.i("Existing draft: %s", draftEntryJson);
            ReplyDraft draftEntry = jsonAdapter.fromJson(draftEntryJson);
            allDrafts.put(entry.getKey(), draftEntry);
          }
          recycleOldDrafts(allDrafts);
        })
        .toSingleDefault(DraftSaveResult.SAVED_OR_UPDATED);
  }

  @VisibleForTesting
  void recycleOldDrafts(Map<String, ReplyDraft> allDrafts) {
    DateTime nowDateTime = DateTime.now(TimeZone.getTimeZone("UTC"));
    DateTime draftDateLimit = nowDateTime.minusDays(recycleDraftsOlderThanNumDays);
    long draftDateLimitMillis = draftDateLimit.getMilliseconds(TimeZone.getTimeZone("UTC"));

    SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();

    for (Map.Entry<String, ReplyDraft> entry : allDrafts.entrySet()) {
      ReplyDraft draftEntry = entry.getValue();
      if (draftEntry.createdTimeMillis() < draftDateLimitMillis) {
        // Stale draft.
        sharedPrefsEditor.remove(entry.getKey());
      }
    }

    sharedPrefsEditor.apply();
  }

  @Override
  public Observable<String> streamDrafts(Identifiable identifiable) {
    return rxSharedPrefs.getString(keyForDraft(identifiable), "")
        .asObservable()
        .map(replyDraftJson -> {
          if ("".equals(replyDraftJson)) {
            // Always emit a default value so that the UI's initial setup is done.
            return "";

          } else {
            ReplyDraft replyDraft = moshi.adapter(ReplyDraft.class).fromJson(replyDraftJson);
            //noinspection ConstantConditions
            return replyDraft.body();
          }
        })
        .distinctUntilChanged();
  }

  @Override
  public Completable removeDraft(Identifiable identifiable) {
    //String parent;
    //if (contribution instanceof Comment) {
    //  parent = ((Comment) contribution).getBody();
    //} else if (contribution instanceof Message) {
    //  parent = ((Message) contribution).getBody();
    //} else if (contribution instanceof Submission) {
    //  parent = ((Submission) contribution).getTitle();
    //} else if (contribution instanceof ContributionFullNameWrapper) {
    //  parent = contribution.getFullName();
    //} else {
    //  throw new UnsupportedOperationException();
    //}
    //Timber.i("Removing draft for %s", parent);
    return Completable.fromAction(() -> sharedPrefs.edit().remove(keyForDraft(identifiable)).apply());
  }

  @VisibleForTesting
  static String keyForDraft(Identifiable contribution) {
    Preconditions.checkNotNull(contribution.getFullName(), "fullname");
    return "replyDraftFor_" + contribution.getFullName();
  }
}
