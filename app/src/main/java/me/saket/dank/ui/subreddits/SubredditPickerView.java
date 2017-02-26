package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;
import rx.functions.Action1;

public class SubredditPickerView extends FrameLayout {

    @BindView(R.id.subredditpicker_subreddit_list) RecyclerView subredditList;
    @BindView(R.id.subredditpicker_load_progress) View subredditsLoadProgressView;
    @BindView(R.id.subredditpicker_refresh_progress) View subredditsRefreshProgressView;

    private SubredditAdapter subredditAdapter;
    private Subscription apiSubscription;

    public static SubredditPickerView showIn(ToolbarExpandableSheet toolbarSheet) {
        SubredditPickerView subredditPickerView = new SubredditPickerView(toolbarSheet.getContext());
        toolbarSheet.addView(subredditPickerView);
        return subredditPickerView;
    }

    public SubredditPickerView(Context context) {
        super(context);
        addView(LayoutInflater.from(context).inflate(R.layout.fragment_subreddit_picker, this, false));
        ButterKnife.bind(this, this);

        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager();
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);
        subredditList.setLayoutManager(flexboxLayoutManager);

        subredditAdapter = new SubredditAdapter();
        subredditList.setAdapter(subredditAdapter);

        apiSubscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> Dank.reddit().userSubreddits())
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .compose(applySchedulers())
                .compose(doOnStartAndFinish(setSubredditLoadProgressVisible()))
                .subscribe(subredditAdapter, logError("Failed to get subreddits"));
    }

// ======== PUBLIC APIs ======== //

    public void setOnSubredditClickListener(OnSubredditClickListener listener) {
        subredditAdapter.setOnSubredditClickListener(listener);
    }

// ======== END PUBLIC APIs ======== //

    @Override
    protected void onDetachedFromWindow() {
        if (apiSubscription != null) {
            apiSubscription.unsubscribe();
        }
        super.onDetachedFromWindow();
    }

    private Action1<Boolean> setSubredditLoadProgressVisible() {
        return visible -> {
            if (visible) {
                if (subredditAdapter.getItemCount() == 0) {
                    subredditsLoadProgressView.setVisibility(View.VISIBLE);
                } else {
                    subredditsRefreshProgressView.setVisibility(View.VISIBLE);
                }

            } else {
                subredditsLoadProgressView.setVisibility(View.GONE);
                subredditsRefreshProgressView.setVisibility(View.GONE);
            }
        };
    }

}
