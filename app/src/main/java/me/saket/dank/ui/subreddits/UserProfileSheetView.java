package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.paginators.Paginator;

import java.util.concurrent.TimeUnit;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.messages.InboxActivity;
import me.saket.dank.utils.Strings;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import timber.log.Timber;

public class UserProfileSheetView extends FrameLayout {

  @BindView(R.id.userprofilesheet_karma) TextView karmaView;
  @BindView(R.id.userprofilesheet_messages) TextView messagesView;

  @BindColor(R.color.userprofile_no_messages) int noMessagesTextColor;
  @BindColor(R.color.userprofile_unread_messages) int unreadMessagesTextColor;

  private Disposable confirmLogoutTimer = Disposables.disposed();
  private Disposable logoutDisposable = Disposables.empty();
  private Disposable userInfoDisposable = Disposables.empty();
  private ToolbarExpandableSheet parentSheet;

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
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    // TODO: 02/03/17 Cache user account.
    // TODO: Get unread messages count from /messages/unread because that
    // TODO: might be cached already when fetching new messages in background.

    karmaView.setText(R.string.userprofile_loading_karma);

    userInfoDisposable = Dank.reddit().loggedInUserAccount()
        .compose(applySchedulersSingle())
        .subscribe(loggedInUser -> {
          populateKarmaCount(loggedInUser);
          populateUnreadMessageCount(loggedInUser);

        }, error -> {
          Timber.e(error, "Couldn't get logged in user info");
          karmaView.setText(R.string.userprofile_error_user_karma_load);
        });
  }

  private void populateKarmaCount(LoggedInAccount loggedInUser) {
    Integer commentKarma = loggedInUser.getCommentKarma();
    Integer linkKarma = loggedInUser.getLinkKarma();
    int karmaCount = commentKarma + linkKarma;

    String compactKarma = Strings.abbreviateCount(karmaCount);
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
    userInfoDisposable.dispose();
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
          .subscribe(__ -> {
            logoutButton.setText(R.string.login_logout);
          });

    } else {
      // Confirm logout was visible when this button was clicked. Logout the user for real.
      confirmLogoutTimer.dispose();
      logoutDisposable.dispose();
      logoutButton.setText(R.string.userprofile_logging_out);

      logoutDisposable = Dank.reddit()
          .logout()
          .compose(applySchedulersCompletable())
          .subscribe(() -> {
            parentSheet.collapse();

          }, error -> {
            logoutButton.setText(R.string.login_logout);
            Timber.e(error, "Logout failure");
          });
    }
  }

}
