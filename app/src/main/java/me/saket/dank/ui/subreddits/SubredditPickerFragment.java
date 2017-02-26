package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.DankFragment;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;
import rx.functions.Action1;

public class SubredditPickerFragment extends DankFragment {

    static final String TAG = SubredditPickerFragment.class.getSimpleName();

    @BindView(R.id.subredditpicker_subreddit_list) RecyclerView subredditList;
    @BindView(R.id.subredditpicker_load_progress) View subredditsLoadProgressView;
    @BindView(R.id.subredditpicker_refresh_progress) View subredditsRefreshProgressView;

    private SubredditAdapter subredditAdapter;
    private ToolbarExpandableSheet toolbarSheet;

    public static SubredditPickerFragment create() {
        return new SubredditPickerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_subreddit_picker, container, false);
        ButterKnife.bind(this, layout);
        toolbarSheet = ((ToolbarExpandableSheet) container);

        // Setup Subreddit list.
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager();
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);
        subredditList.setLayoutManager(flexboxLayoutManager);

        subredditAdapter = new SubredditAdapter();
        subredditAdapter.setOnSubredditClickListener(subreddit -> {
            // TODO: 26/02/17 Open subreddit.
            toolbarSheet.hide();
            getActivity().setTitle(subreddit.getDisplayName().toLowerCase(Locale.ENGLISH));
        });
        subredditList.setAdapter(subredditAdapter);

        return  layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        // TODO: 26/02/17 Get default subreddits for non-logged in users.

        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> Dank.reddit().userSubreddits())
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .compose(applySchedulers())
                .compose(doOnStartAndFinish(setSubredditLoadProgressVisible()))
                .subscribe(subredditAdapter, logError("Failed to get subreddits"));
        unsubscribeOnDestroy(subscription);
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
