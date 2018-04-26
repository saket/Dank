package me.saket.dank.ui.preferences;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.view.RxView;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.inflationx.viewpump.InflateResult;
import io.reactivex.Observable;

@Singleton
public class TypefaceInflationInterceptor implements io.github.inflationx.viewpump.Interceptor {

  private final Observable<TypefaceResource> preferenceChanges;

  @Inject
  public TypefaceInflationInterceptor(Preference<TypefaceResource> preference) {
    preferenceChanges = preference.asObservable()
        .replay(1)
        .refCount();
  }

  @Override
  public InflateResult intercept(Chain chain) {
    InflateResult result = chain.proceed(chain.request());
    View view = result.view();

    if (view instanceof TextView) {
      applyTypefaceChanges((TextView) view);
    }

    return result;
  }

  public void applyTypefaceChanges(TextView view) {
    preferenceChanges
        .takeUntil(RxView.detaches(view))
        .subscribe(typefaceRes -> {
          Typeface font = typefaceRes.get(view.getResources());
          view.setTypeface(font);
        });
  }
}
