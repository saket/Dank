package me.saket.dank.ui;

import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.R;

/**
 * Base class for all activities.
 */
public class DankActivity extends AppCompatActivity {

    private CompositeDisposable onStopDisposables;
    private CompositeDisposable onDestroyDisposables;

    @Override
    public void onStop() {
        if (onStopDisposables != null) {
            onStopDisposables.clear();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (onDestroyDisposables != null) {
            onDestroyDisposables.clear();
        }
        super.onDestroy();
    }

    protected void unsubscribeOnStop(Disposable subscription) {
        if (onStopDisposables == null) {
            onStopDisposables = new CompositeDisposable();
        }
        onStopDisposables.add(subscription);
    }

    protected void unsubscribeOnDestroy(Disposable subscription) {
        if (onDestroyDisposables == null) {
            onDestroyDisposables = new CompositeDisposable();
        }
        onDestroyDisposables.add(subscription);
    }

    protected void findAndSetupToolbar() {
        setSupportActionBar(ButterKnife.findById(this, R.id.toolbar));
    }

    /**
     * Because suppressing the null check everywhere is stupid.
     */
    @NonNull
    @Override
    public ActionBar getSupportActionBar() {
        //noinspection ConstantConditions
        return super.getSupportActionBar();
    }

}
