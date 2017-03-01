package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.DankSubreddit;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class SubredditPickerView extends FrameLayout {

    @BindView(R.id.subredditpicker_search) EditText searchView;
    @BindView(R.id.subredditpicker_subreddit_list) RecyclerView subredditList;
    @BindView(R.id.subredditpicker_load_progress) View subredditsLoadProgressView;
    @BindView(R.id.subredditpicker_refresh_progress) View subredditsRefreshProgressView;

    @BindString(R.string.frontpage_subreddit_name) String frontpageSubredditName;

    private SubredditAdapter subredditAdapter;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    // TODO: 28/02/17 Cache this somewhere else.
    private static List<DankSubreddit> userSubreddits;

    public static SubredditPickerView showIn(ToolbarExpandableSheet toolbarSheet) {
        SubredditPickerView subredditPickerView = new SubredditPickerView(toolbarSheet.getContext());
        toolbarSheet.addView(subredditPickerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return subredditPickerView;
    }

    public SubredditPickerView(Context context) {
        super(context);
        View pickerView = LayoutInflater.from(context).inflate(R.layout.view_subreddit_picker, this, false);

        ViewGroup.LayoutParams params = pickerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        addView(pickerView, params);
        ButterKnife.bind(this, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager();
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        flexboxLayoutManager.setAlignItems(AlignItems.STRETCH);
        subredditList.setLayoutManager(flexboxLayoutManager);

        subredditAdapter = new SubredditAdapter();
        subredditList.setAdapter(subredditAdapter);
        subredditList.setItemAnimator(null);

        if (userSubreddits != null) {
            setSubredditLoadProgressVisible().call(false);
            subredditAdapter.updateData(userSubreddits);

        } else {
            Subscription apiSubscription = (Dank.reddit().isUserLoggedIn() ? loggedInSubreddits() : loggedOutSubreddits())
                    .compose(doOnStartAndFinish(setSubredditLoadProgressVisible()))
                    .map(subreddits -> {
                        subreddits.add(0, DankSubreddit.createFrontpage(frontpageSubredditName));
                        return subreddits;
                    })
                    .doOnNext(subreddits -> userSubreddits = subreddits)
                    .subscribe(subredditAdapter, logError("Failed to get subreddits"));
            subscriptions.add(apiSubscription);
        }

        setupSearch();
    }

    public void setOnSubredditClickListener(OnSubredditClickListener listener) {
        subredditAdapter.setOnSubredditClickListener(listener);
    }

// ======== END PUBLIC APIs ======== //

    private Observable<List<DankSubreddit>> loggedInSubreddits() {
        return Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> Dank.reddit().userSubreddits())
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .compose(applySchedulers());
    }

    public Observable<List<DankSubreddit>> loggedOutSubreddits() {
        List<String> defaultSubreddits = Arrays.asList(getResources().getStringArray(R.array.default_subreddits));
        ArrayList<DankSubreddit> dankSubreddits = new ArrayList<>();
        for (String subredditName : defaultSubreddits) {
            dankSubreddits.add(DankSubreddit.create(subredditName));
        }
        return Observable.just(dankSubreddits);
    }

    private void setupSearch() {
        Subscription subscription = RxTextView.textChanges(searchView)
                .skip(1)
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(searchTerm -> {
                    // Since CharSequence is mutable, it's important
                    // to create a copy before doing a debounce().
                    return searchTerm.toString();
                })
                .map(searchTerm -> {
                    if (searchTerm.isEmpty()) {
                        return userSubreddits;
                    }

                    boolean unknownSubreddit = false;

                    List<DankSubreddit> filteredSubreddits = new ArrayList<>(userSubreddits.size());
                    for (DankSubreddit userSubreddit : userSubreddits) {
                        if (userSubreddit.displayName().toLowerCase(Locale.ENGLISH).contains(searchTerm.toLowerCase())) {
                            filteredSubreddits.add(userSubreddit);

                            if (userSubreddit.displayName().equalsIgnoreCase(searchTerm)) {
                                unknownSubreddit = true;
                            }
                        }
                    }

                    if (!unknownSubreddit) {
                        filteredSubreddits.add(DankSubreddit.create(searchTerm));
                    }

                    return filteredSubreddits;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subredditAdapter, logError("Search error"));
        subscriptions.add(subscription);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (subscriptions != null) {
            subscriptions.clear();
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
