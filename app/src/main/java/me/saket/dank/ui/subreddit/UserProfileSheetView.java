package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.dean.jraw.models.Account;
import net.dean.jraw.pagination.Paginator;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.UserProfileRepository;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.ui.user.messages.InboxActivity;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.applySchedulers;

public class UserProfileSheetView extends FrameLayout {

  @BindView(R.id.userprofilesheet_karma) TextView karmaView;
  @BindView(R.id.userprofilesheet_messages) TextView messagesView;
  @BindView(R.id.userprofilesheet_refresh_progress) ProgressBar refreshProgressView;

  @BindColor(R.color.userprofile_no_messages) int noMessagesTextColor;
  @BindColor(R.color.userprofile_unread_messages) int unreadMessagesTextColor;

  @Inject Lazy<UserProfileRepository> userProfileRepository;
  @Inject Lazy<InboxRepository> inboxRepository;
  @Inject Lazy<ErrorResolver> errorResolver;
  @Inject Lazy<UserSessionRepository> userSessionRepository;

  private Disposable confirmLogoutTimer = Disposables.disposed();
  private Disposable logoutDisposable = Disposables.empty();
  private ToolbarExpandableSheet parentSheet;
  private LifecycleOwnerViews.Streams lifecycle;

  public static UserProfileSheetView showIn(ToolbarExpandableSheet toolbarSheet) {
    UserProfileSheetView subredditPickerView = new UserProfileSheetView(toolbarSheet.getContext());
    toolbarSheet.addView(subredditPickerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    subredditPickerView.setParentSheet(toolbarSheet);
    return subredditPickerView;
  }

  public UserProfileSheetView(Context context) {
    super(context);
    inflate(context, R.layout.view_user_profile_sheet, this);
    ButterKnife.bind(this, this);
    Dank.dependencyInjector().inject(this);

    lifecycle = LifecycleOwnerViews.create(this, ((LifecycleOwnerActivity) getContext()).lifecycle());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    Timber.i("Fetching user profile for user profile sheet");
    Observable<Account> replayedUserAccount = userProfileRepository.get()
        .loggedInUserAccounts()
        .subscribeOn(io())
        .replay()
        .refCount();

    replayedUserAccount
        .observeOn(mainThread())
        .doOnSubscribe(o -> karmaView.setText(R.string.userprofile_loading_karma))
        .takeUntil(lifecycle.viewDetaches())
        .subscribe(
            this::populateKarmaCount,
            error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't get logged in user's account"));

              karmaView.setText(R.string.userprofile_error_user_karma_load);
            });

    Observable<Integer> unreadCountFromInbox = inboxRepository.get()
        .messages(InboxFolder.UNREAD)
        .subscribeOn(io())
        .map(unreads -> unreads.size());

    // TODO JRAW
    Observable<Integer> unreadCountFromAccount = replayedUserAccount
        //.map(account -> account.getInboxCount());
        .flatMap(o -> Observable.never());

    unreadCountFromInbox
        .takeUntil(unreadCountFromAccount)
        .concatWith(unreadCountFromAccount)
        .onErrorResumeNext(Observable.empty())
        .observeOn(mainThread())
        .takeUntil(lifecycle.viewDetaches())
        .subscribe(
            this::populateUnreadMessageCount,
            error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't update unread message count"));
            });

    // This call is intentionally allowed to outlive this View's lifecycle.
    // Additionally, NYT-Store blocks multiple calls so it's safe to refresh
    // even if the previous get also triggered a network call.
    userProfileRepository.get()
        .refreshLoggedInUserAccount()
        .subscribeOn(io())
        .doOnError(error -> {
          if (error instanceof NoSuchElementException) {
            // Possibly a misconfiguration of Store.
            return;
          }
          ResolvedError resolvedError = errorResolver.get().resolve(error);
          resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't refresh user"));
        })
        .onErrorComplete()
        .andThen(Observable.just(GONE))
        .startWith(VISIBLE)
        .observeOn(mainThread())
        .subscribe(refreshProgressView::setVisibility);
  }

  private void populateKarmaCount(Account loggedInUser) {
    Integer commentKarma = loggedInUser.getCommentKarma();
    Integer linkKarma = loggedInUser.getLinkKarma();
    int karmaCount = commentKarma + linkKarma;

    String compactKarma = Strings.abbreviateScore(karmaCount);
    karmaView.setText(getResources().getString(R.string.userprofile_karma_count, compactKarma));
  }

  private void populateUnreadMessageCount(int unreadMessageCount) {
    if (unreadMessageCount == 0) {
      messagesView.setText(R.string.userprofile_messages);

    } else if (unreadMessageCount == 1) {
      messagesView.setText(getResources().getString(R.string.userprofile_unread_messages_count_single, unreadMessageCount));

    } else if (unreadMessageCount < Paginator.RECOMMENDED_MAX_LIMIT) {
      messagesView.setText(getResources().getString(R.string.userprofile_unread_messages_count_99_or_less, unreadMessageCount));

    } else {
      messagesView.setText(R.string.userprofile_unread_messages_count_99_plus);
    }
    messagesView.setTextColor(unreadMessageCount > 0 ? unreadMessagesTextColor : noMessagesTextColor);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    logoutDisposable.dispose();
  }

  public void setParentSheet(ToolbarExpandableSheet parentSheet) {
    this.parentSheet = parentSheet;
  }

  @OnClick(R.id.userprofilesheet_messages)
  void onClickMessages(View messageButton) {
    InboxActivity.start(getContext());

    // Hide this sheet once it gets fully covered.
    postDelayed(
        () -> parentSheet.collapse(),
        (long) (IndependentExpandablePageLayout.ANIMATION_DURATION_MILLIS * 1.6f)
    );
  }

  @OnClick(R.id.userprofilesheet_comments)
  void onClickComments() {
  }

  @OnClick(R.id.userprofilesheet_submissions)
  void onClickSubmissions() {
  }

  @OnClick(R.id.userprofilesheet_logout)
  void onClickLogout(TextView logoutButton) {
    if (confirmLogoutTimer.isDisposed()) {
      logoutButton.setText(R.string.userprofile_confirm_logout);
      confirmLogoutTimer = Observable.timer(5, TimeUnit.SECONDS)
          .compose(applySchedulers())
          .subscribe(o -> logoutButton.setText(R.string.login_logout));

    } else {
      // Confirm logout was visible when this button was clicked. Logout the user for real.
      confirmLogoutTimer.dispose();
      logoutDisposable.dispose();
      logoutButton.setText(R.string.userprofile_logging_out);

      logoutDisposable = userSessionRepository.get().logout()
          .subscribeOn(io())
          .observeOn(mainThread())
          .subscribe(
              () -> parentSheet.collapse(),
              error -> {
                logoutButton.setText(R.string.login_logout);

                ResolvedError resolvedError = errorResolver.get().resolve(error);
                resolvedError.ifUnknown(() -> Timber.e(error, "Logout failure"));
              }
          );
    }
  }
}
