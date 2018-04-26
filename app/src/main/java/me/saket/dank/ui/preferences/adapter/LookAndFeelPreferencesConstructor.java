package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.MultiOptionPreferencePopup;
import me.saket.dank.ui.preferences.TypefaceResource;

public class LookAndFeelPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Preference<TypefaceResource> typefacePref;
  private final Preference<Boolean> showSubmissionThumbnails;

  @Inject
  public LookAndFeelPreferencesConstructor(
      Preference<TypefaceResource> typefacePref,
      @Named("show_submission_thumbnails") Preference<Boolean> showSubmissionThumbnails)
  {
    this.typefacePref = typefacePref;
    this.showSubmissionThumbnails = showSubmissionThumbnails;
  }

  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    if (BuildConfig.DEBUG) {
      TypefaceResource typefaceResource = typefacePref.get();

      uiModels.add(UserPreferenceSectionHeader.UiModel.create("Text"));
      uiModels.add(UserPreferenceButton.UiModel.create(
          "Typeface",
          typefaceResource.name(),
          (clickHandler, event) -> {
            MultiOptionPreferencePopup.Builder<TypefaceResource> popupBuilder = MultiOptionPreferencePopup.builder(typefacePref)
                .addOption(TypefaceResource.DEFAULT, R.string.userprefs_typeface_roboto, R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Bifocals", R.font.bifocals, "bifocals.otf"),
                    R.string.userprefs_typeface_bifocals,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir book", -1, "avenir_book.ttf"),
                    R.string.userprefs_typeface_avenir_book,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir light", -1, "avenir_light.ttf"),
                    R.string.userprefs_typeface_avenir_light,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir medium", -1, "avenir_medium.ttf"),
                    R.string.userprefs_typeface_avenir_medium,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir roman", -1, "avenir_roman.ttf"),
                    R.string.userprefs_typeface_avenir_roman,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir next regular", -1, "avenir_next_regular.ttf"),
                    R.string.userprefs_typeface_avenir_next,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir condensed regular", -1, "AvenirNextCondensed-Regular.ttf"),
                    R.string.userprefs_typeface_avenir_condensed_regular,
                    R.drawable.ic_text_fields_20dp)
                .addOption(
                    TypefaceResource.create("Avenir condensed medium", -1, "AvenirNextCondensed-Medium.ttf"),
                    R.string.userprefs_typeface_avenir_condensed_medium,
                    R.drawable.ic_text_fields_20dp)
                ;
            clickHandler.show(popupBuilder, event.itemViewHolder());
          }));
    }

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_subreddit)));

    uiModels.add(UserPreferenceSwitch.UiModel.create(
        c.getString(R.string.userprefs_submission_thumbnails),
        showSubmissionThumbnails.get()
            ? c.getString(R.string.userprefs_submission_thumbnail_summary_on)
            : c.getString(R.string.userprefs_submission_thumbnail_summary_off),
        showSubmissionThumbnails.get(),
        showSubmissionThumbnails));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_gestures)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_submission_gestures),
        "Options and save. Upvote and downvote.",
        (clickHandler, event) -> clickHandler.expandNestedPage(
            R.layout.view_user_preferences_submission_gestures,
            event.itemViewHolder())));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_comment_gestures),
        "Options and save. Upvote and downvote.",
        (clickHandler, event) -> clickHandler.expandNestedPage(
            R.layout.view_user_preferences_submission_gestures,
            event.itemViewHolder())));

    return uiModels;
  }
}
