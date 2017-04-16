package me.saket.dank.ui;

import com.trello.navi.component.support.NaviDialogFragment;
import com.trello.rxlifecycle.android.FragmentEvent;
import com.trello.rxlifecycle.navi.NaviLifecycle;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Base class for fragments.
 */
public class DankDialogFragment extends NaviDialogFragment {

    private CompositeSubscription onStopSubscriptions;
    private CompositeSubscription onDestroySubscriptions;

    @Override
    public void onStop() {
        if (onStopSubscriptions != null) {
            onStopSubscriptions.clear();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (onDestroySubscriptions != null) {
            onDestroySubscriptions.clear();
        }

        super.onDestroy();
    }

    protected void unsubscribeOnStop(Subscription subscription) {
        if (onStopSubscriptions == null) {
            onStopSubscriptions = new CompositeSubscription();
        }
        onStopSubscriptions.add(subscription);
    }

    protected void unsubscribeOnDestroy(Subscription subscription) {
        if (onDestroySubscriptions == null) {
            onDestroySubscriptions = new CompositeSubscription();
        }
        onDestroySubscriptions.add(subscription);
    }

    public Observable<FragmentEvent> lifecycleEvents() {
        return NaviLifecycle.createFragmentLifecycleProvider(this).lifecycle();
    }

}
