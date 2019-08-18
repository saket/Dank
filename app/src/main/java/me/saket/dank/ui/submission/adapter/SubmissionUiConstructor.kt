package me.saket.dank.ui.submission.adapter

import android.content.Context
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import android.text.Html
import android.text.style.ForegroundColorSpan
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.BuildConfig
import me.saket.dank.R
import me.saket.dank.data.ResolvedError
import me.saket.dank.reply.ReplyRepository
import me.saket.dank.ui.preferences.gestures.submissions.SubmissionSwipeActionsRepository
import me.saket.dank.ui.submission.BookmarksRepository
import me.saket.dank.ui.submission.ParentThread
import me.saket.dank.ui.submission.SubmissionAndComments
import me.saket.dank.ui.submission.SubmissionCommentTreeUiConstructor
import me.saket.dank.ui.submission.SubmissionContentLoadError
import me.saket.dank.ui.user.UserSessionRepository
import me.saket.dank.urlparser.Link
import me.saket.dank.urlparser.RedditSubmissionLink
import me.saket.dank.utils.CombineLatestWithLog
import me.saket.dank.utils.CombineLatestWithLog.O
import me.saket.dank.utils.CommentSortUtils
import me.saket.dank.utils.DankSubmissionRequest
import me.saket.dank.utils.Dates
import me.saket.dank.utils.JrawUtils2
import me.saket.dank.utils.Optional
import me.saket.dank.utils.Pair
import me.saket.dank.utils.Strings
import me.saket.dank.utils.Themes
import me.saket.dank.utils.Truss
import me.saket.dank.utils.lifecycle.LifecycleStreams
import me.saket.dank.utils.markdown.Markdown
import me.saket.dank.vote.VotingManager
import me.saket.dank.widgets.span.RoundedBackgroundSpan
import me.saket.dank.widgets.swipe.SwipeActions
import net.dean.jraw.models.Submission
import timber.log.Timber
import java.util.ArrayList
import java.util.Collections
import javax.inject.Inject

class SubmissionUiConstructor @Inject constructor(
    private val contentLinkUiModelConstructor: SubmissionContentLinkUiConstructor,
    private val replyRepository: ReplyRepository,
    private val votingManager: VotingManager,
    private val markdown: Markdown,
    private val userSessionRepository: UserSessionRepository,
    private val bookmarksRepository: Lazy<BookmarksRepository>,
    private val swipeActionsRepository: SubmissionSwipeActionsRepository
) {

  @CheckResult
  fun stream(
      context: Context,
      submissionCommentTreeUiConstructor: SubmissionCommentTreeUiConstructor,
      optionalSubmissionDatum2: Observable<Optional<SubmissionAndComments>>,
      submissionRequests: Observable<DankSubmissionRequest>,
      contentLinks: Observable<Optional<Link>>,
      mediaContentLoadErrors: Observable<Optional<SubmissionContentLoadError>>,
      commentsLoadErrors: Observable<Optional<ResolvedError>>
  ): Observable<List<SubmissionScreenUiModel>> {

    val sharedOptionalSubmissionDatum = optionalSubmissionDatum2
        .replay(1)
        .refCount()

    return sharedOptionalSubmissionDatum
        .distinctUntilChanged { prev, next -> prev.isPresent == next.isPresent }
        .switchMap { optional ->
          Timber.i("Constructing submission: %s", optional.map { (submission) -> submission.title })

          if (optional.isEmpty) {
            return@switchMap sharedOptionalSubmissionDatum
                .distinctUntilChanged { prev, next -> prev.isPresent == next.isPresent }
                .switchMap {
                  commentsLoadErrors
                      .map<List<SubmissionScreenUiModel>> {
                        when {
                          it.isPresent -> listOf(SubmissionCommentsLoadError.UiModel.create(it.get()))
                          else -> emptyList()
                        }
                      }
                }
          }

          val sharedSubmissionDatum2 = sharedOptionalSubmissionDatum
              // Not sure why, but the parent switchMap() on submission change gets triggered
              // after this chain receives an empty submission, so adding this extra takeWhile().
              // Update: I think it's because switchMap() acts like a flatMap() if the nested Rx
              // stream is synchronous.
              .takeWhile { it.isPresent }
              .map { it.get() }
              .startWith(optional.get())
              .replay(1)
              .refCount()

          val contentLinkUiModels = contentLinks
              .distinctUntilChanged()
              .withLatestFrom(sharedSubmissionDatum2.map { it.submission })
              .doOnDispose { contentLinkUiModelConstructor.clearGlideTargets(context) }
              .switchMap { (contentLink, submission) ->
                val crosspostLinkUiModel = crosspostLinkUiModel(context, submission)
                when {
                  crosspostLinkUiModel.isPresent -> Observable.just(crosspostLinkUiModel)
                  contentLink.isEmpty -> Observable.just(Optional.empty())
                  else -> contentLinkUiModelConstructor
                    .streamLoad(context, contentLink.get(), ImageWithMultipleVariants.of(submission.preview))
                    .doOnError { e -> Timber.e(e, "Error while creating content link ui model") }
                    .distinctUntilChanged()
                    .map { Optional.of(it) }
                }
              }

          val submissionPendingSyncReplyCounts = sharedSubmissionDatum2
              .map { it.submission }
              .take(1)  // replace flatMap -> switchMap if we expect more than 1 emissions.
              .flatMap {
                replyRepository
                    .streamPendingSyncReplies(ParentThread.of(it))
                    .subscribeOn(io())
              }
              .map { it.size }
              .startWith(0)  // Stream sometimes takes too long to emit anything.

          val externalChanges = Observable
              .merge(votingManager.streamChanges(), bookmarksRepository.get().streamChanges())
              .startWith(NOTHING)

          val headerUiModels = CombineLatestWithLog.from<Context, Submission, Optional<SubmissionContentLinkUiModel>, SwipeActions, SubmissionCommentsHeader.UiModel>(
              O.of("ext-change", externalChanges.map { context }),
              O.of("submission 2", sharedSubmissionDatum2.map { it.submission }.distinctUntilChanged()),
              O.of("content-link", contentLinkUiModels),
              O.of("swipe-actions", swipeActionsRepository.swipeActions.distinctUntilChanged()),
              ::headerUiModel
          )

          val commentOptionsUiModels = CombineLatestWithLog.from<Submission, DankSubmissionRequest, Int, Any, SubmissionCommentOptions.UiModel>(
              O.of("submission 3", sharedSubmissionDatum2.map { it.submission }.distinctUntilChanged()),
              O.of("submission requests", submissionRequests),
              O.of("pending-sync-reply-count", submissionPendingSyncReplyCounts),
              ::commentOptionsUiModel
          )

          val contentLoadErrorUiModels = mediaContentLoadErrors
              .map { optionalError -> optionalError.map { error -> error.uiModel(context) } }

          val viewFullThreadUiModels = submissionRequests
              .map {
                when {
                  it.focusCommentId() == null -> Optional.empty()
                  else -> Optional.of(SubmissionCommentsViewFullThread.UiModel.create(it))
                }
              }
              .startWith(Optional.empty())  // submissionRequests stream sometimes takes too long to emit anything.

          val commentRowUiModels = submissionCommentTreeUiConstructor
              .stream(
                  context,
                  sharedSubmissionDatum2,
                  submissionRequests,
                  io())
              .startWith(emptyList<SubmissionScreenUiModel>())

          val commentsLoadProgressUiModels = Observables
              .combineLatest(sharedSubmissionDatum2, commentsLoadErrors)
              .map { (submissionData, commentsLoadErrors) ->
                val comments = submissionData.comments
                val commentsLoadingFailed = commentsLoadErrors.isPresent
                when {
                  comments.isEmpty && !commentsLoadingFailed -> Optional.of(SubmissionCommentsLoadProgress.UiModel.create())
                  else -> Optional.empty()
                }
              }
              .startWith(Optional.of(SubmissionCommentsLoadProgress.UiModel.create()))

          val commentsLoadErrorUiModels = commentsLoadErrors
              .map { optionalError -> optionalError.map { error -> SubmissionCommentsLoadError.UiModel.create(error) } }

          CombineLatestWithLog.from(
              O.of("header", headerUiModels),
              O.of("comment options", commentOptionsUiModels),
              O.of("content-load-error", contentLoadErrorUiModels),
              O.of("view-full-thread", viewFullThreadUiModels),
              O.of("comments-load-progress", commentsLoadProgressUiModels),
              O.of("comments-load-error", commentsLoadErrorUiModels),
              O.of("comment-rows", commentRowUiModels)
          ) { header, commentOptions, optionalContentError, viewFullThread, optionalCommentsLoadProgress, optionalCommentsLoadError, commentModels ->
            // Steps to update this list:
            // 1. Update the initial capacity.
            // 2. Ensure that the ordering is correct. This is
            //    the same order in which they'll be displayed.
            val allItems = ArrayList<SubmissionScreenUiModel>(6 + commentModels.size)

            allItems.add(header)
            optionalContentError.ifPresent { allItems.add(it) }
            viewFullThread.ifPresent { allItems.add(it) }
            allItems.add(commentOptions)
            allItems.addAll(commentModels)

            if (BuildConfig.DEBUG) {
              Timber.i("Received %s comment ui models", commentModels.size)
            }

            // Comments progress and error go after comment rows
            // so that inline reply for submission appears above them.
            optionalCommentsLoadProgress.ifPresent { allItems.add(it) }
            optionalCommentsLoadError.ifPresent { allItems.add(it) }
            Collections.unmodifiableList(allItems)
          }
        }
  }

  /**
   * Header contains submission details, content link and self-text post.
   */
  private fun headerUiModel(
      context: Context,
      submission: Submission,
      contentLinkUiModel: Optional<SubmissionContentLinkUiModel>,
      swipeActions: SwipeActions
  ): SubmissionCommentsHeader.UiModel {
    val pendingOrDefaultVote = votingManager.getPendingOrDefaultVote(submission, submission.vote)
    val voteDirectionColor = Themes.voteColor(pendingOrDefaultVote)
    val adapterId = JrawUtils2.generateAdapterId(submission)


    val selfTextOptional = when {
      submission.isSelfPost && !submission.selfText!!.isEmpty() -> Optional.of(markdown.parseSelfText(submission))
      else -> Optional.empty()
    }

    val vote = votingManager.getScoreAfterAdjustingPendingVote(submission)

    var titleBuilder = Truss()

    if (submission.isArchived || submission.isLocked) {
      val tag = when {
        submission.isArchived -> context.getString(R.string.submission_header_archived)
        else -> context.getString(R.string.submission_header_locked)
      }

      titleBuilder = titleBuilder
          .pushSpan(RoundedBackgroundSpan(
              color(context, R.color.yellow_600),
              color(context, R.color.black_opacity_75),
              context.resources.getDimensionPixelSize(R.dimen.spacing2),
              context.resources.getDimensionPixelSize(R.dimen.textsize12),
              context.resources.getDimensionPixelSize(R.dimen.spacing16),
              context.resources.getDimensionPixelSize(R.dimen.spacing8)
          ))
          .append(tag.toUpperCase())
          .popSpan()
          .append("\n")
          .append("\n")
    }

    titleBuilder = titleBuilder
        .pushSpan(ForegroundColorSpan(color(context, voteDirectionColor)))
        .append(Strings.abbreviateScore(vote.toFloat()))
        .popSpan()
        .append("  ")
        .append(Html.fromHtml(submission.title))

    val byline = context.getString(
        R.string.submission_byline,
        submission.subreddit,
        submission.author,
        Dates.createTimestamp(context.resources, submission.created.getTime()))

    return SubmissionCommentsHeader.UiModel.builder()
        .adapterId(adapterId)
        .title(titleBuilder.build(), Pair.create(vote, pendingOrDefaultVote))
        .byline(byline)
        .selfText(selfTextOptional)
        .optionalContentLinkModel(contentLinkUiModel)
        .submission(submission)
        .isSaved(bookmarksRepository.get().isSaved(submission))
        .swipeActions(swipeActions)
        .build()
  }

  private fun crosspostLinkUiModel(
    context: Context,
    submission: Submission
  ): Optional<SubmissionContentLinkUiModel> {
    val crosspostParent = submission.crosspostParents?.firstOrNull() ?: return Optional.empty()
    return Optional.of(SubmissionContentLinkUiModel.builder()
      .title(crosspostParent.title)
      .titleMaxLines(2)
      .titleTextColorRes(R.color.submission_link_title)
      .byline(context.getString(R.string.submission_crosspost, crosspostParent.subreddit))
      .bylineTextColorRes(R.color.submission_link_byline)
      .icon(Optional.ofNullable(context.getDrawable(R.drawable.ic_subreddits_24dp)))
      .iconBackgroundRes(Optional.empty())
      .thumbnail(Optional.empty())
      .backgroundTintColor(Optional.empty())
      .progressVisible(false)
      .link(RedditSubmissionLink.create(crosspostParent.url, crosspostParent.id, crosspostParent.subreddit))
      .build())
  }

  private fun commentOptionsUiModel(
      submission: Submission,
      request: DankSubmissionRequest,
      pendingSyncReplyCount: Int
  ): SubmissionCommentOptions.UiModel {
    val postedAndPendingCommentCount = submission.commentCount!! + pendingSyncReplyCount
    val abbreviatedCommentCount = Strings.abbreviateScore(postedAndPendingCommentCount.toFloat())
    val sortTextRes = CommentSortUtils.sortingDisplayTextRes(request.commentSort().mode())
    return SubmissionCommentOptions.UiModel.create(abbreviatedCommentCount, sortTextRes)
  }

  companion object {

    private val NOTHING = LifecycleStreams.NOTHING

    @ColorInt
    private fun color(context: Context, @ColorRes colorRes: Int): Int {
      return ContextCompat.getColor(context, colorRes)
    }
  }
}
