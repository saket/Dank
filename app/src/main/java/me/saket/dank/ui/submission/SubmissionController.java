package me.saket.dank.ui.submission;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Message;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.ui.UiChange;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy;
import me.saket.dank.ui.submission.events.MarkMessageAsReadRequested;
import me.saket.dank.ui.submission.events.SubmissionChangeCommentSortClicked;
import me.saket.dank.ui.submission.events.SubmissionChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentSortChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentsLoadFailed;
import me.saket.dank.ui.submission.events.SubmissionCommentsRefreshClicked;
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
import me.saket.dank.ui.submission.events.SubmissionViewFullCommentsClicked;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import timber.log.Timber;

public class SubmissionController implements ObservableTransformer<UiEvent, UiChange<SubmissionUi>> {

  private Lazy<SubmissionRepository> submissionRepository;
  private Lazy<InboxRepository> inboxRepository;

  @Inject
  public SubmissionController(Lazy<SubmissionRepository> submissionRepository, Lazy<InboxRepository> inboxRepository) {
    this.submissionRepository = submissionRepository;
    this.inboxRepository = inboxRepository;
  }

  @Override
  public Observable<UiChange<SubmissionUi>> apply(Observable<UiEvent> events) {
    // The replay count is set to 1 intentionally. I was seeing issues where
    // an Observable passed to a switchMap() would replay all events since
    // the app started.
    Observable<UiEvent> replayedEvents = events.replay(1).refCount();

    //noinspection unchecked
    return Observable.mergeArray(
        contentProgressToggles(replayedEvents),
        changeSortPopupShows(replayedEvents),
        sortModeChanges(replayedEvents),
        manualRefreshes(replayedEvents),
        fullThreadLoads(replayedEvents),
        markMessageAsReads(replayedEvents)
    );
  }

  // WARNING: If we ever use this progress toggle for indicating loading of submission,
  // the calls to show() and hide() will need to be synchronized.
  private Observable<UiChange<SubmissionUi>> contentProgressToggles(Observable<UiEvent> events) {
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

    Observable<?> progressShows = Observable.mergeArray(
        fullSubmissionLoadStarts,
        contentResolveStarts,
        mediaLoadStarts);

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

    Observable<?> progressHides = Observable.mergeArray(
        submissionLoadFails,
        contentResolveCompletes,
        contentResolveFails,
        mediaLoadSucceeds,
        mediaLoadFails,
        nsfwContentFilters);

    return Observable.merge(
        progressShows.map(o -> ui -> ui.showProgress()),
        progressHides.map(o1 -> ui -> ui.hideProgress()));
  }

  private Observable<UiChange<SubmissionUi>> changeSortPopupShows(Observable<UiEvent> events) {
    Observable<DankSubmissionRequest> requestChanges = events
        .ofType(SubmissionRequestChanged.class)
        .map(event -> event.request());

    return events
        .ofType(SubmissionChangeCommentSortClicked.class)
        .withLatestFrom(requestChanges, Pair::create)
        .map(pair -> ui -> ui.showChangeSortPopup(pair.first(), pair.second()));
  }

  private Observable<UiChange<SubmissionUi>> sortModeChanges(Observable<UiEvent> events) {
    Observable<DankSubmissionRequest> requests = events
        .ofType(SubmissionRequestChanged.class)
        .map(event -> event.request());

    return events
        .ofType(SubmissionCommentSortChanged.class)
        .map(event -> event.selectedSort())
        .withLatestFrom(requests, Pair::create)
        .map(pair -> {
          CommentSort selectedSort = pair.first();
          DankSubmissionRequest lastRequest = pair.second();
          return lastRequest.toBuilder()
              .commentSort(selectedSort, SelectedBy.USER)
              .build();
        })
        .map(newRequest -> ui -> ui.acceptRequest(newRequest));
  }

  private Observable<UiChange<SubmissionUi>> manualRefreshes(Observable<UiEvent> events) {
    Observable<DankSubmissionRequest> requests = events
        .ofType(SubmissionRequestChanged.class)
        .map(event -> event.request());

    Observable<SubmissionCommentsRefreshClicked> refreshClicks = events
        .ofType(SubmissionCommentsRefreshClicked.class);

    return refreshClicks
        .withLatestFrom(requests, (__, req) -> req)
        .flatMap(request ->
            submissionRepository.get()
                .clearCachedSubmissionComments(request)
                .andThen(Observable.never()));
  }

  private Observable<UiChange<SubmissionUi>> fullThreadLoads(Observable<UiEvent> events) {
    Observable<DankSubmissionRequest> requestChanges = events
        .ofType(SubmissionRequestChanged.class)
        .map(event -> event.request());

    Observable<SubmissionViewFullCommentsClicked> viewFullThreadClicks = events
        .ofType(SubmissionViewFullCommentsClicked.class);

    Observable<Boolean> openedFromMessages = events
        .ofType(MarkMessageAsReadRequested.class)
        .map(o -> true)
        .startWith(false);

    return viewFullThreadClicks
        .withLatestFrom(requestChanges, (__, request) -> request)
        .map(request -> request.toBuilder()
            .focusCommentId(null)
            .contextCount(null)
            .build())
        .withLatestFrom(openedFromMessages, Pair::create)
        .flatMap(pair -> {
          DankSubmissionRequest updatedRequest = pair.first();
          Boolean wasSubmissionOpenedFromMessages = pair.second();

          // If the user is viewing a focused comment, the comments must have
          // gotten updated, so they're being force invalidated here.
          if (wasSubmissionOpenedFromMessages) {
            return submissionRepository.get()
                .clearCachedSubmissionComments(updatedRequest)
                .andThen(Observable.just(ui -> ui.acceptRequest(updatedRequest)));
          } else {
            return Observable.just(ui -> ui.acceptRequest(updatedRequest));
          }
        });
  }

  private Observable<UiChange<SubmissionUi>> markMessageAsReads(Observable<UiEvent> events) {
    Observable<Message> readRequests = events
        .ofType(MarkMessageAsReadRequested.class)
        .map(event -> event.message());

    return events
        .ofType(SubmissionChanged.class)
        .map(event -> event.optionalSubmission())
        .filter(Optional::isPresent)
        .map(Optional::get)
        // The message for which this submission was opened will
        // only be marked as read once the comments have been fetched.
        .filter(submission -> submission.getComments().isPresent())
        .withLatestFrom(readRequests, (__, message) -> message)
        .flatMapCompletable(message -> inboxRepository.get().setRead(message, true))
        .andThen(Observable.never());
  }

  private void log(String message, Object... args) {
    //Timber.i(message, args);
  }
}
