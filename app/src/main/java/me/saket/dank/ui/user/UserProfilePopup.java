package me.saket.dank.ui.user;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTarget;

import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Account;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;

import java.util.Date;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import timber.log.Timber;

public class UserProfilePopup extends PopupWindowWithMaterialTransition {

  @BindView(R.id.userprofilepopup_profile_image) ImageView profileImageView;
  @BindView(R.id.userprofilepopup_username) TextView usernameView;
  @BindView(R.id.userprofilepopup_stats_viewflipper) ViewFlipper statsViewFlipper;
  @BindView(R.id.userprofilepopup_account_age) TextView accountAgeView;
  @BindView(R.id.userprofilepopup_link_karma) TextView linkKarmaView;
  @BindView(R.id.userprofilepopup_comment_karma) TextView commentKarmaView;
  @BindView(R.id.userprofilepopup_stats_load_error_message) TextView errorStateMessageView;
  @BindView(R.id.userprofilepopup_send_private_message) Button messageButton;
  @BindView(R.id.userprofilepopup_view_full_profile) Button viewFullProfileButton;

  private CompositeDisposable onDismissDisposables = new CompositeDisposable();

  @Inject ErrorResolver errorResolver;
  @Inject UserProfileRepository userProfileRepository;

  private enum StatsLoadState {
    IN_FLIGHT,
    ERROR,
    FETCHED
  }

  @SuppressLint("InflateParams")
  public UserProfilePopup(Context context) {
    super(context);
    Dank.dependencyInjector().inject(this);

    View popupView = LayoutInflater.from(context).inflate(R.layout.popup_user_profile, null);
    ButterKnife.bind(this, popupView);
    setContentView(popupView);

    setOnDismissListener(() -> onDismissDisposables.clear());
  }

  public void loadUserProfile(RedditUserLink userLink) {
    String username = userLink.name();
    profileImageView.setContentDescription(profileImageView.getResources().getString(R.string.cd_userprofilepopup_profile_image, username));
    usernameView.setText(usernameView.getResources().getString(R.string.user_name_u_prefix, username));

    profileImageView.setVisibility(View.GONE);

    showStatsLoadState(StatsLoadState.IN_FLIGHT);

    onDismissDisposables.add(
        userProfileRepository.profile(userLink)
            .compose(RxUtils.applySchedulersSingle())
            .subscribe(
                userProfile -> {
                  showStatsLoadState(StatsLoadState.FETCHED);

                  Account userAccount = userProfile.account();
                  linkKarmaView.setText(Strings.abbreviateScore(userAccount.getLinkKarma()));
                  commentKarmaView.setText(Strings.abbreviateScore(userAccount.getCommentKarma()));
                  accountAgeView.setText(constructAccountAge(userAccount.getCreated()));

                  if (userProfile.userSubreddit() != null) {
                    // Replace fade-in with a scale animation.
                    LayoutTransition layoutTransition = ((ViewGroup) profileImageView.getParent()).getLayoutTransition();
                    layoutTransition.disableTransitionType(LayoutTransition.APPEARING);
                    layoutTransition.setDuration(200);
                    profileImageView.setVisibility(View.VISIBLE);

                    //noinspection ConstantConditions
                    Glide.with(profileImageView)
                        .load(userProfile.userSubreddit().profileImageUrl())
                        .apply(new RequestOptions()
                            .centerCrop()
                            .transform(new GlideCircularTransformation())
                        )
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(new ImageViewTarget<Drawable>(profileImageView) {
                          @Override
                          protected void setResource(Drawable resource) {
                            profileImageView.setImageDrawable(resource);

                            profileImageView.setScaleX(0);
                            profileImageView.setScaleY(0);
                            profileImageView.setVisibility(View.VISIBLE);
                            profileImageView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setInterpolator(Animations.INTERPOLATOR)
                                .start();
                          }
                        });
                  }
                },
                error -> {
                  showStatsLoadState(StatsLoadState.ERROR);

                  if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 404) {
                    // User not found.
                    errorStateMessageView.setText(R.string.userprofilepopup_user_not_found);
                    messageButton.setEnabled(false);
                    viewFullProfileButton.setEnabled(false);

                  } else {
                    ResolvedError resolvedError = errorResolver.resolve(error);
                    if (resolvedError.isUnknown()) {
                      Timber.e(error, "Couldn't fetch profile for: %s", userLink.unparsedUrl());
                    }

                    Resources resources = errorStateMessageView.getResources();
                    String errorMessage = resources.getString(resolvedError.errorMessageRes());
                    String tapToRetryText = resources.getString(R.string.userprofilepopup_error_message_tap_to_retry);
                    if (!errorMessage.endsWith(resources.getString(R.string.userprofilepopup_error_message_full_period))) {
                      errorMessage += resources.getString(R.string.userprofilepopup_error_message_full_period);
                    }
                    errorMessage += " " + tapToRetryText;
                    errorStateMessageView.setText(errorMessage);
                    errorStateMessageView.setOnClickListener(o -> loadUserProfile(userLink));
                  }
                })
    );
  }

  public void showWithAnchor(View anchorView) {
    super.showWithAnchor(anchorView, Gravity.TOP | Gravity.START);
  }

  public void showAtLocation(View anchorView, Point location) {
    super.showAtLocation(anchorView, Gravity.TOP | Gravity.START, location);
  }

  private String constructAccountAge(Date acctCreationDate) {
    LocalDate accountCreationDate = Instant.ofEpochMilli(acctCreationDate.getTime()).atZone(ZoneId.of("UTC")).toLocalDate();
    LocalDate nowDate = LocalDate.now(ZoneId.of("UTC"));
    Period period = accountCreationDate.until(nowDate);
    Resources resources = accountAgeView.getResources();

    if (period.getMonths() < 0) {
      return resources.getString(R.string.userprofilepopup_account_age_in_days, period.getDays());
    } else if (period.getYears() < 0) {
      return resources.getString(R.string.userprofilepopup_account_age_in_months, period.getMonths());
    } else {
      return resources.getString(R.string.userprofilepopup_account_age_in_years, period.getYears());
    }
  }

  private void showStatsLoadState(StatsLoadState loadState) {
    @IdRes int stateViewIdRes;

    switch (loadState) {
      case IN_FLIGHT:
        stateViewIdRes = R.id.userprofilepopup_stats_load_progress;
        break;

      case ERROR:
        stateViewIdRes = R.id.userprofilepopup_stats_load_error_message;
        break;

      case FETCHED:
        stateViewIdRes = R.id.userprofilepopup_stats_container;
        break;

      default:
        throw new AssertionError("Unknown state: " + loadState);
    }

    statsViewFlipper.setDisplayedChild(statsViewFlipper.indexOfChild(ButterKnife.findById(statsViewFlipper, stateViewIdRes)));
  }

  @OnClick(R.id.userprofilepopup_send_private_message)
  void onClickSendPrivateMessage(Button b) {
    Toast.makeText(b.getContext(), R.string.work_in_progress, Toast.LENGTH_SHORT).show();
  }

  @OnClick(R.id.userprofilepopup_view_full_profile)
  void onClickViewFullProfile(Button b) {
    Toast.makeText(b.getContext(), R.string.work_in_progress, Toast.LENGTH_SHORT).show();
  }
}
