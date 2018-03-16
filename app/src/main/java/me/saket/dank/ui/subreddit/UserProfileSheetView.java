package me.saket.dank.ui.subreddit;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.paginators.Paginator;

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
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.UserProfileRepository;
import me.saket.dank.ui.user.messages.InboxActivity;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import timber.log.Timber;

public class UserProfileSheetView extends FrameLayout {

  @BindView(R.id.userprofilesheet_karma) TextView karmaView;
  @BindView(R.id.userprofilesheet_messages) TextView messagesView;
  @BindView(R.id.userprofilesheet_refresh_progress) ProgressBar refreshProgressView;

  @BindColor(R.color.userprofile_no_messages) int noMessagesTextColor;
  @BindColor(R.color.userprofile_unread_messages) int unreadMessagesTextColor;

  @Inject Lazy<UserProfileRepository> userProfileRepository;
  //@Inject Lazy<InboxRepository> inboxRepository;
  @Inject Lazy<ErrorResolver> errorResolver;

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

    // TODO: Get unread messages count from /messages/unread because that might be cached already when fetching new messages in background.

    karmaView.setText(R.string.userprofile_loading_karma);

    userProfileRepository.get().loggedInUserAccounts()
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(lifecycle.viewDetaches())
        .subscribe(
            loggedInUser -> {
              populateKarmaCount(loggedInUser);
              populateUnreadMessageCount(loggedInUser);
            },
            error -> {
              Timber.e(error, "Couldn't get logged in user info");
              karmaView.setText(R.string.userprofile_error_user_karma_load);
            });

    // This call is intentionally allowed to outlive this View's lifecycle.
    // Additionally, NYT-Store blocks multiple calls so it's safe to refresh
    // even if the previous get also triggered a network call.
    userProfileRepository.get()
        .refreshLoggedInUserAccount()
        .subscribeOn(io())
        .andThen(Observable.just(GONE))
        .startWith(VISIBLE)
        .observeOn(mainThread())
        .subscribe(
            refreshProgressView::setVisibility,
            error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't refresh user"));
            }
        );
  }

  private void populateKarmaCount(LoggedInAccount loggedInUser) {
    Integer commentKarma = loggedInUser.getCommentKarma();
    Integer linkKarma = loggedInUser.getLinkKarma();
    int karmaCount = commentKarma + linkKarma;

    String compactKarma = Strings.abbreviateScore(karmaCount);
    karmaView.setText(getResources().getString(R.string.userprofile_karma_count, compactKarma));
  }

  private void populateUnreadMessageCount(LoggedInAccount loggedInUser) {
    int inboxCount = loggedInUser.getInboxCount();
    if (inboxCount == 0) {
      messagesView.setText(R.string.userprofile_messages);

    } else if (inboxCount == 1) {
      messagesView.setText(getResources().getString(R.string.userprofile_unread_messages_count_single, inboxCount));

    } else if (inboxCount < Paginator.RECOMMENDED_MAX_LIMIT) {
      messagesView.setText(getResources().getString(R.string.userprofile_unread_messages_count_99_or_less, inboxCount));

    } else {
      messagesView.setText(R.string.userprofile_unread_messages_count_99_plus);
    }
    messagesView.setTextColor(inboxCount > 0 ? unreadMessagesTextColor : noMessagesTextColor);
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
        (long) (IndependentExpandablePageLayout.ANIMATION_DURATION * 1.6f)
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

      logoutDisposable = Dank.reddit().logout()
          .compose(applySchedulersCompletable())
          .subscribe(
              () -> parentSheet.collapse(),
              error -> {
                logoutButton.setText(R.string.login_logout);
                Timber.e(error, "Logout failure");
              }
          );
    }
  }
}
