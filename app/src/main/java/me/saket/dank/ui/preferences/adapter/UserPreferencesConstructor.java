package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import me.saket.dank.ui.preferences.UserPreferenceGroup;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.Optional;

public class UserPreferencesConstructor {

  interface ChildConstructor {
    List<UserPreferencesScreenUiModel> construct(Context c);
  }

  private final Observable<Object> userPrefChanges;
  private final Map<UserPreferenceGroup, ChildConstructor> childConstructors;

  @Inject
  public UserPreferencesConstructor(
      LookAndFeelPreferencesConstructor lookAndFeel,
      FiltersPreferencesConstructor filters,
      DataUsagePreferencesConstructor dataUsage,
      MiscellaneousPreferencesConstructor miscellaneous,
      AboutDankPreferencesConstructor aboutDank,
      @Named("user_preferences") Observable<Object> userPrefChanges)
  {
    this.userPrefChanges = userPrefChanges;

    childConstructors = new HashMap<>();
    childConstructors.put(UserPreferenceGroup.LOOK_AND_FEEL, lookAndFeel);
    childConstructors.put(UserPreferenceGroup.FILTERS, filters);
    childConstructors.put(UserPreferenceGroup.DATA_USAGE, dataUsage);
    childConstructors.put(UserPreferenceGroup.MISCELLANEOUS, miscellaneous);
    childConstructors.put(UserPreferenceGroup.ABOUT_DANK, aboutDank);
  }

  public Observable<List<UserPreferencesScreenUiModel>> stream(Context c, Optional<UserPreferenceGroup> group) {
    if (group.isEmpty()) {
      return Observable.just(Collections.emptyList());
    } else {
      return userPrefChanges
          .map(o -> childConstructors.get(group.get()).construct(c))
          .as(Arrays2.immutable());
    }
  }
}
