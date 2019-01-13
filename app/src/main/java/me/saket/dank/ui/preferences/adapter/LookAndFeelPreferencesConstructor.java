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
  private final Preference<Boolean> showCommentCountInByline;
  private final Preference<Boolean> showSubmissionThumbnailsOnLeft;

  @Inject
  public LookAndFeelPreferencesConstructor(
      Preference<TypefaceResource> typefacePref,
      @Named("show_submission_thumbnails") Preference<Boolean> showSubmissionThumbnails,
      @Named("comment_count_in_submission_list_byline") Preference<Boolean> showCommentCountInByline,
      @Named("show_submission_thumbnails_on_left") Preference<Boolean> showSubmissionThumbnailsOnLeft
  ) {
    this.typefacePref = typefacePref;
    this.showSubmissionThumbnails = showSubmissionThumbnails;
    this.showCommentCountInByline = showCommentCountInByline;
    this.showSubmissionThumbnailsOnLeft = showSubmissionThumbnailsOnLeft;
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
            MultiOptionPreferencePopup.Builder<TypefaceResource> builder = MultiOptionPreferencePopup.builder(typefacePref);
            add(builder, TypefaceResource.create("Roboto", "roboto_regular.ttf"));
            add(builder, TypefaceResource.create("Roboto condensed", "RobotoCondensed-Regular.ttf"));

            add(builder, TypefaceResource.create("Bifocals", "bifocals.otf"));

            add(builder, TypefaceResource.create("Transcript regular", "TranscriptTrial-Regular.otf"));

            add(builder, TypefaceResource.create("Basis", "basis-grotesque-trial-regular.otf"));
            add(builder, TypefaceResource.create("Basis medium", "basis-grotesque-trial-medium.otf"));

            add(builder, TypefaceResource.create("Relative book", "relativetrial-book.otf"));
            add(builder, TypefaceResource.create("Relative faux", "relativetrial-faux.otf"));
            add(builder, TypefaceResource.create("Relative medium", "relativetrial-medium.otf"));
            add(builder, TypefaceResource.create("Relative mono 12", "relativetrial-mono12pitch.otf"));

            add(builder, TypefaceResource.create("LaFabrique", "LaFabriqueTrial-Regular.otf"));
            add(builder, TypefaceResource.create("LaFabrique SemiBold", "LaFabriqueTrial-SemiBold.otf"));

            add(builder, TypefaceResource.create("Reader", "readertrial-regular.otf"));
            add(builder, TypefaceResource.create("Reader medium", "readertrial-medium.otf"));

            add(builder, TypefaceResource.create("Inter", "Inter-UI-Regular.otf"));
            add(builder, TypefaceResource.create("Inter medium", "Inter-UI-Medium.otf"));

            add(builder, TypefaceResource.create("Visuelt", "visuelttrial-regular.otf"));

            clickHandler.show(builder, event.itemViewHolder());
          }));
    }

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_subreddit)));

    uiModels.add(new UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_submission_thumbnails),
        showSubmissionThumbnails.get()
            ? c.getString(R.string.userprefs_submission_thumbnail_summary_on)
            : c.getString(R.string.userprefs_submission_thumbnail_summary_off),
        showSubmissionThumbnails.get(),
        showSubmissionThumbnails));

    uiModels.add(new UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_show_submission_thumbnails_on_left),
        showSubmissionThumbnailsOnLeft.get()
            ? c.getString(R.string.userprefs_show_submission_thumbnails_on_left_summary_on)
            : c.getString(R.string.userprefs_show_submission_thumbnails_on_left_summary_off),
        showSubmissionThumbnailsOnLeft.get(),
        showSubmissionThumbnailsOnLeft,
        showSubmissionThumbnails.get()));

    uiModels.add(new UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_item_byline_comment_count),
        showCommentCountInByline.get()
            ? c.getString(R.string.userprefs_item_byline_comment_count_summary_on)
            : c.getString(R.string.userprefs_item_byline_comment_count_summary_off),
        showCommentCountInByline.get(),
        showCommentCountInByline));

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
