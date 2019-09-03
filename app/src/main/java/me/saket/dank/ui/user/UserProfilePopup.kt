package me.saket.dank.ui.user

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.IdRes
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.ImageViewTarget
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.R
import me.saket.dank.data.ErrorResolver
import me.saket.dank.di.Dank
import me.saket.dank.urlparser.RedditUserLink
import me.saket.dank.utils.Animations
import me.saket.dank.utils.RxPopupWindow
import me.saket.dank.utils.Strings
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@SuppressLint("InflateParams")
class UserProfilePopup constructor(context: Context) : PopupWindowWithMaterialTransition(context) {

  @BindView(R.id.userprofilepopup_profile_image)
  lateinit var profileImageView: ImageView

  @BindView(R.id.userprofilepopup_username)
  lateinit var usernameView: TextView

  @BindView(R.id.userprofilepopup_stats_viewflipper)
  lateinit var statsViewFlipper: ViewFlipper

  @BindView(R.id.userprofilepopup_account_age)
  lateinit var accountAgeView: TextView

  @BindView(R.id.userprofilepopup_link_karma)
  lateinit var linkKarmaView: TextView

  @BindView(R.id.userprofilepopup_comment_karma)
  lateinit var commentKarmaView: TextView

  @BindView(R.id.userprofilepopup_stats_load_error_message)
  lateinit var errorStateMessageView: TextView

  @BindView(R.id.userprofilepopup_send_private_message)
  lateinit var messageButton: Button

  @BindView(R.id.userprofilepopup_view_full_profile)
  lateinit var viewFullProfileButton: Button

  @Inject
  lateinit var errorResolver: Lazy<ErrorResolver>

  @Inject
  lateinit var userProfileRepository: Lazy<UserProfileRepository>

  private enum class StatsLoadState {
    IN_FLIGHT,
    ERROR,
    FETCHED
  }

  init {
    Dank.dependencyInjector().inject(this)

    val popupView = LayoutInflater.from(context).inflate(R.layout.popup_user_profile, null)
    ButterKnife.bind(this, popupView)
    contentView = popupView
  }

  override fun calculateTransitionEpicenter(anchor: View, popupDecorView: ViewGroup, showLocation: Point): Rect {
    // Set the epicenter at (x,y).
    return Rect(showLocation.x, showLocation.y, showLocation.x, showLocation.y)
  }

  fun loadUserProfile(userLink: RedditUserLink) {
    val username = userLink.name()

    profileImageView.contentDescription = profileImageView.resources.getString(R.string.cd_userprofilepopup_profile_image, username)
    usernameView.text = usernameView.resources.getString(R.string.user_name_u_prefix, username)
    profileImageView.visibility = View.GONE
    showStatsLoadState(StatsLoadState.IN_FLIGHT)

    userProfileRepository.get().profile(userLink)
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(RxPopupWindow.dismisses(this).ignoreElements())
        .subscribe({ result ->
          when (result) {
            is UserProfile -> renderUserProfile(result)
            is UserNotFound -> showUserNotFoundError()
            is UserSuspended -> showUserSuspended()
            is UnexpectedError -> showUnexpectedError(result.error, userLink)
          }
        }, { error ->
          val resolvedError = errorResolver.get().resolve(error)
          resolvedError.ifUnknown { Timber.e(error, "Couldn't get user profile") }
        })
  }

  private fun renderUserProfile(userProfile: UserProfile) {
    showStatsLoadState(StatsLoadState.FETCHED)

    val userAccount = userProfile.account
    linkKarmaView.text = Strings.abbreviateScore(userAccount.linkKarma.toFloat())
    commentKarmaView.text = Strings.abbreviateScore(userAccount.commentKarma.toFloat())
    accountAgeView.text = constructAccountAge(userAccount.created)

    if (userAccount.icon != null) {
      // Replace fade-in with a scale animation.
      val layoutTransition = (profileImageView.parent as ViewGroup).layoutTransition
      layoutTransition.disableTransitionType(LayoutTransition.APPEARING)
      layoutTransition.setDuration(200)
      profileImageView.visibility = View.VISIBLE

      Glide.with(profileImageView)
          .load(userAccount.icon)
          .apply(RequestOptions()
              .centerCrop()
              .circleCrop())
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(object : ImageViewTarget<Drawable>(profileImageView) {
            override fun setResource(resource: Drawable?) {
              profileImageView.setImageDrawable(resource)

              profileImageView.scaleX = 0f
              profileImageView.scaleY = 0f
              profileImageView.visibility = View.VISIBLE
              profileImageView.animate()
                  .scaleX(1f)
                  .scaleY(1f)
                  .setInterpolator(Animations.INTERPOLATOR)
                  .start()
            }
          })
    }
  }

  private fun showUserNotFoundError() {
    showStatsLoadState(StatsLoadState.ERROR)

    errorStateMessageView.setText(R.string.userprofilepopup_user_not_found)
    messageButton.isEnabled = false
    viewFullProfileButton.isEnabled = false
  }

  private fun showUserSuspended() {
    showStatsLoadState(StatsLoadState.ERROR)

    errorStateMessageView.setText(R.string.userprofilepopup_user_not_found)
    messageButton.isEnabled = false
    viewFullProfileButton.isEnabled = false
  }

  private fun showUnexpectedError(error: Throwable, userLink: RedditUserLink) {
    showStatsLoadState(StatsLoadState.ERROR)

    val resolvedError = errorResolver.get().resolve(error)
    if (resolvedError.isUnknown) {
      Timber.e(error, "Couldn't fetch profile for: %s", userLink.unparsedUrl())
    }

    val resources = errorStateMessageView.resources
    var errorMessage = resources.getString(resolvedError.errorMessageRes())
    val tapToRetryText = resources.getString(R.string.userprofilepopup_error_message_tap_to_retry)
    if (!errorMessage.endsWith(resources.getString(R.string.userprofilepopup_error_message_period))) {
      errorMessage += resources.getString(R.string.userprofilepopup_error_message_period)
    }
    errorMessage += " $tapToRetryText"
    errorStateMessageView.text = errorMessage
    errorStateMessageView.setOnClickListener { loadUserProfile(userLink) }
  }

  // Used only for testing.
  fun showWithAnchor(anchorView: View) {
    super.showWithAnchor(anchorView, Gravity.TOP or Gravity.START)
  }

  fun showAtLocation(anchorView: View, location: Point) {
    super.showAtLocation(anchorView, Gravity.NO_GRAVITY, location)
  }

  private fun constructAccountAge(acctCreationDate: Date): String {
    val accountCreationDate = Instant.ofEpochMilli(acctCreationDate.time).atZone(ZoneId.of("UTC")).toLocalDate()
    val nowDate = LocalDate.now(ZoneId.of("UTC"))
    val period = accountCreationDate.until(nowDate)
    val resources = accountAgeView.resources

    return when {
      period.months < 0 -> resources.getString(R.string.userprofilepopup_account_age_in_days, period.days)
      period.years < 0 -> resources.getString(R.string.userprofilepopup_account_age_in_months, period.months)
      else -> resources.getString(R.string.userprofilepopup_account_age_in_years, period.years)
    }
  }

  private fun showStatsLoadState(loadState: StatsLoadState) {
    @IdRes val stateViewIdRes: Int = when (loadState) {
      StatsLoadState.IN_FLIGHT -> R.id.userprofilepopup_stats_load_progress
      StatsLoadState.ERROR -> R.id.userprofilepopup_stats_load_error_message
      StatsLoadState.FETCHED -> R.id.userprofilepopup_stats_container
    }

    statsViewFlipper.displayedChild = statsViewFlipper.indexOfChild(statsViewFlipper.findViewById(stateViewIdRes))
  }

  @OnClick(R.id.userprofilepopup_send_private_message)
  internal fun onClickSendPrivateMessage(b: Button) {
    Toast.makeText(b.context, R.string.work_in_progress, Toast.LENGTH_SHORT).show()
  }

  @OnClick(R.id.userprofilepopup_view_full_profile)
  internal fun onClickViewFullProfile(b: Button) {
    Toast.makeText(b.context, R.string.work_in_progress, Toast.LENGTH_SHORT).show()
  }
}
