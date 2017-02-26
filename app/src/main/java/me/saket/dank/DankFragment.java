package me.saket.dank;

import android.support.v4.app.Fragment;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Base class for fragments.
 */
public class DankFragment extends Fragment {

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

}
