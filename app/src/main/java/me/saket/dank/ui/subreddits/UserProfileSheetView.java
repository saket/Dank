package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class UserProfileSheetView extends FrameLayout {

    @BindView(R.id.userprofilesheet_karma) TextView karmaView;

    private ToolbarExpandableSheet parentSheet;
    private Subscription confirmLogoutTimer;
    private Subscription logoutSubscription = Subscriptions.empty();

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

        // TODO: 02/03/17 Cache user account.

        karmaView.setText(R.string.loading_karma);
        Dank.reddit().loggedInUserAccount()
                .compose(applySchedulers())
                .subscribe(loggedInUser -> {
                    Integer commentKarma = loggedInUser.getCommentKarma();
                    Integer linkKarma = loggedInUser.getLinkKarma();
                    int karmaCount = commentKarma + linkKarma;

                    String compactKarma;
                    if (karmaCount < 1_000) {
                        compactKarma = String.valueOf(karmaCount);
                    } else if (karmaCount < 1_000_000) {
                        compactKarma = karmaCount / 1_000 + "k";
                    } else {
                        compactKarma = karmaCount / 1_000_000 + "m";
                    }
                    karmaView.setText(getResources().getString(R.string.karma_count, compactKarma));

                }, error -> {
                    Timber.e(error, "Couldn't get logged in user info");
                    karmaView.setText(R.string.error_user_karma_load);
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        logoutSubscription.unsubscribe();
    }

    public void setParentSheet(ToolbarExpandableSheet parentSheet) {
        this.parentSheet = parentSheet;
    }

    @OnClick(R.id.userprofilesheet_messages)
    void onClickMessages() {
        // TODO: 02/03/17
        parentSheet.collapse();
    }

    @OnClick(R.id.userprofilesheet_comments)
    void onClickComments() {
        // TODO: 02/03/17
        parentSheet.collapse();
    }

    @OnClick(R.id.userprofilesheet_submissions)
    void onClickSubmissions() {
        // TODO: 02/03/17
        parentSheet.collapse();
    }

    @OnClick(R.id.userprofilesheet_logout)
    void onClickLogout(TextView logoutButton) {
        if (confirmLogoutTimer == null || confirmLogoutTimer.isUnsubscribed()) {
            logoutButton.setText(R.string.confirm_logout);
            confirmLogoutTimer = Observable.timer(5, TimeUnit.SECONDS)
                    .compose(applySchedulers())
                    .subscribe(__ -> {
                        logoutButton.setText(R.string.logout);
                    });

        } else {
            // Confirm logout was visible when this button was clicked. Logout the user for real.
            confirmLogoutTimer.unsubscribe();
            logoutSubscription.unsubscribe();
            logoutButton.setText(R.string.logging_out);

            logoutSubscription = Dank.reddit()
                    .logout()
                    .compose(applySchedulers())
                    .subscribe(__ -> {
                        parentSheet.collapse();

                    }, error -> {
                        logoutButton.setText(R.string.logout);
                        Timber.e(error, "Logout failure");
                    });
        }
    }

}
