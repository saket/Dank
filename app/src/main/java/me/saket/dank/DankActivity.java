package me.saket.dank;

import android.app.Activity;

import butterknife.ButterKnife;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Base class for all activities.
 */
public class DankActivity extends Activity {

    private CompositeSubscription onStopSubscriptions;
    private CompositeSubscription onDestroySubscriptions;

    @Override
    protected void onStop() {
        if (onStopSubscriptions != null) {
            onStopSubscriptions.clear();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
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

    protected void findAndSetupToolbar(boolean showUpButton) {
        setActionBar(ButterKnife.findById(this, R.id.toolbar));
        if (showUpButton) {
            //noinspection ConstantConditions
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

}
