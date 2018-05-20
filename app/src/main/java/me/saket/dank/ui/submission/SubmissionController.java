package me.saket.dank.ui.submission;

import static io.reactivex.schedulers.Schedulers.io;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import me.saket.dank.ui.UiChange;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.events.SubmissionChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentSortChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentsLoadFailed;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingCompleted;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingFailed;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingStarted;
import me.saket.dank.ui.submission.events.SubmissionImageLoadStarted;
import me.saket.dank.ui.submission.events.SubmissionImageLoadSucceeded;
import me.saket.dank.ui.submission.events.SubmissionMediaLoadFailed;
import me.saket.dank.ui.submission.events.SubmissionNsfwContentFiltered;
import me.saket.dank.ui.submission.events.SubmissionRequestChanged;
import me.saket.dank.ui.submission.events.SubmissionVideoLoadStarted;
import me.saket.dank.ui.submission.events.SubmissionVideoLoadSucceeded;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Pair;
import timber.log.Timber;

public class SubmissionController implements ObservableTransformer<UiEvent, UiChange<SubmissionUi>> {

  @Inject
  public SubmissionController() {
  }

  @Override
  public Observable<UiChange<SubmissionUi>> apply(Observable<UiEvent> events) {
    // The replay count is set to 1 intentionally. I was seeing issues where
    // an Observable passed to a switchMap() would replay all events since
    // the app started.
    Observable<UiEvent> replayedEvents = events.replay(1).refCount();

    //noinspection unchecked
    return Observable.mergeArray(progressToggles(replayedEvents));
  }

  private Observable<UiChange<SubmissionUi>> progressToggles(Observable<UiEvent> events) {
//    Observable<SubmissionPageLifecycleChanged> pageCollapses = events
//        .ofType(SubmissionPageLifecycleChanged.class)
//        .filter(event -> event.state() == PageState.COLLAPSED);
//
//    Observable<SubmissionPopulateReceived> submissionPopulates = events
//        .ofType(SubmissionPopulateReceived.class);

    Observable<SubmissionChanged> submissionChanges = events
        .ofType(SubmissionChanged.class);

    Observable fullSubmissionLoadStarts = events
        .ofType(SubmissionRequestChanged.class)
        .withLatestFrom(submissionChanges, Pair::create)
        .filter(pair -> pair.second().optionalSubmission().isEmpty())
        .doOnNext(o -> log("Full submission load started"));

    Observable<?> contentResolveStarts = events
        .ofType(SubmissionContentResolvingStarted.class)
        .doOnNext(o -> log("Resolving content"));

    Observable<?> mediaLoadStarts = Observable.merge(
        events.ofType(SubmissionImageLoadStarted.class),
        events.ofType(SubmissionVideoLoadStarted.class))
        .doOnNext(o -> log("Loading media"));

    Observable<?> sortChangeStarted = events
        .ofType(SubmissionCommentSortChanged.class)
        .doOnNext(o -> log("Sort changed"));

    Observable<?> progressShows = Observable.mergeArray(
        fullSubmissionLoadStarts,
        contentResolveStarts,
        mediaLoadStarts,
        sortChangeStarted);

    Observable<?> submissionLoadFails = events
        .ofType(SubmissionCommentsLoadFailed.class)
        .doOnNext(o -> log("Submission load failed"));

    Observable<?> contentResolveCompletes = events
        .ofType(SubmissionContentResolvingCompleted.class)
        .filter(event -> event.willBeDisplayedAsAContentLink())
        .doOnNext(o -> log("Content resolved as a content link"));

    Observable<?> contentResolveFails = events
        .ofType(SubmissionContentResolvingFailed.class)
        .doOnNext(o -> Timber.i("media resolve failed"));

    Observable<?> mediaLoadSucceeds = Observable.merge(
        events.ofType(SubmissionImageLoadSucceeded.class),
        events.ofType(SubmissionVideoLoadSucceeded.class))
        .doOnNext(o -> log("Media load succeeded"));

    Observable<?> mediaLoadFails = events
        .ofType(SubmissionMediaLoadFailed.class)
        .doOnNext(o -> log("Media load failed"));

    Observable<?> nsfwContentFilters = events
        .ofType(SubmissionNsfwContentFiltered.class)
        .doOnEach(o -> log("NSFW content filtered"));

    Observable<?> sortingChangeCompleted = events
        .ofType(SubmissionCommentSortChanged.class)
        .map(event -> event.selectedSort())
        .switchMap(selectedSort -> submissionChanges
            .subscribeOn(io())
            .filter(event -> event.optionalSubmission().isPresent())
            .map(event -> event.optionalSubmission().get())
            .map(submission -> submission.getComments())
            .map(comments -> JrawUtils.commentSortingOf(comments))
            .filter(sortMode -> sortMode == selectedSort)
            .cast(Object.class)
            .onErrorResumeNext(throwable -> {
              Timber.e(throwable, "Error while getting submission's sort");
              // The return value isn't used.
              return Observable.just(new Object());
            })
        );

    Observable<?> progressHides = Observable.mergeArray(
        submissionLoadFails,
        contentResolveCompletes,
        contentResolveFails,
        mediaLoadSucceeds,
        mediaLoadFails,
        nsfwContentFilters,
        sortingChangeCompleted);

    return Observable.merge(
        progressShows.map(o -> ui -> ui.showProgress()),
        progressHides.map(o1 -> ui -> ui.hideProgress()));
  }

  private void log(String message, Object... args) {
    //Timber.i(message, args);
  }
}
