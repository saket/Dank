package me.saket.dank.ui.preferences;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankFragment;

/**
 * Uses custom layouts for preference items because they customizing them + having custom design & controls is easier.
 */
public class PreferencesFragment extends DankFragment {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.userpreferences_viewswitcher) ViewSwitcher layoutSwitcher;

    public static PreferencesFragment create() {
        return new PreferencesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.fragment_user_preferences, container, false);
        ButterKnife.bind(this, layout);

        toolbar.setNavigationOnClickListener(v -> ((UserPreferencesActivity) getActivity()).onClickPreferencesToolbarUp());

        return layout;
    }

    public void populatePreferences(DankPreferenceGroup preferenceGroup) {
        @IdRes int groupLayoutRes = -1;

        switch (preferenceGroup) {
            case LOOK_AND_FEEL:
                break;

            case MANAGE_SUBREDDITS:
                break;

            case FILTERS:
                break;

            case DATA_USAGE:
                break;

            case MISCELLANEOUS:
                groupLayoutRes = R.id.userpreferences_container_misc;
                break;

            case ABOUT_DANK:
                break;
        }

        if (groupLayoutRes != -1) {
            layoutSwitcher.setDisplayedChild(layoutSwitcher.indexOfChild(layoutSwitcher.findViewById(groupLayoutRes)));
        }
    }

}
