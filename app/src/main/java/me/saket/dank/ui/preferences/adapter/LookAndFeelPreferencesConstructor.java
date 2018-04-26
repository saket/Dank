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
            MultiOptionPreferencePopup.Builder<TypefaceResource> popupBuilder = MultiOptionPreferencePopup.builder(typefacePref);
            add(popupBuilder, TypefaceResource.create("Bifocals", "bifocals.otf"));
            add(popupBuilder, TypefaceResource.create("Avenir book", "avenir_book.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir light", "avenir_light.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir medium", "avenir_medium.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir roman", "avenir_roman.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir next regular", "avenir_next_regular.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir condensed regular", "AvenirNextCondensed-Regular.ttf"));
            add(popupBuilder, TypefaceResource.create("Avenir condensed medium", "AvenirNextCondensed-Medium.ttf"));

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

  private void add(MultiOptionPreferencePopup.Builder<TypefaceResource> builder, TypefaceResource resource) {
    builder.addOption(resource, resource.name(), R.drawable.ic_text_fields_20dp);
  }
}
