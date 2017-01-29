package me.saket.dank;

import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import butterknife.BindView;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.RxUtils;
import rx.Subscription;
import timber.log.Timber;

public class SubRedditActivity extends DankActivity {

    @BindView(R.id.subreddit_post_list) RecyclerView postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findAndSetupToolbar(false);

        SubredditPaginator frontPagePaginator = Dank.reddit().frontPagePaginator();

        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(frontPagePaginator.next()))
                .compose(RxUtils.applySchedulers())
                .subscribe(submissions -> {
                    for (Submission submission : submissions) {
                        Timber.i(submission.getTitle());
                    }

                }, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

}
