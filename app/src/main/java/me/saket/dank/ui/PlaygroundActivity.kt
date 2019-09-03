package me.saket.dank.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.PopupMenu
import butterknife.ButterKnife
import butterknife.OnClick
import me.saket.dank.R
import me.saket.dank.di.Dank
import me.saket.dank.ui.user.UserProfilePopup
import me.saket.dank.urlparser.RedditUserLink
import me.saket.dank.urlparser.UrlParser
import javax.inject.Inject

class PlaygroundActivity : DankPullCollapsibleActivity() {

  @Inject
  lateinit var urlParser: UrlParser

  override fun onCreate(savedInstanceState: Bundle?) {
    Dank.dependencyInjector().inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_playground)
    ButterKnife.bind(this)
    findAndSetupToolbar()

    setupContentExpandablePage(findViewById(R.id.playground_root))
    expandFromBelowToolbar()
  }

  @OnClick(R.id.playground_user_with_subreddit)
  internal fun onClickShowProfilePopup1(button: Button) {
    showPopup(button, "http://reddit.com/u/kn0thing")
  }

  @OnClick(R.id.playground_banned_user)
  internal fun onClickShowProfilePopup2(button: Button) {
    showPopup(button, "http://reddit.com/u/TheFlintASteel")
  }

  @OnClick(R.id.playground_non_existent_user)
  internal fun onClickShowProfilePopup3(button: Button) {
    showPopup(button, "http://reddit.com/u/Sak3tme")
  }

  @OnClick(R.id.playground_deleted_user)
  internal fun onClickShowProfilePopup4(button: Button) {
    showPopup(button, "http://reddit.com/u/mosnuk123")
  }

  private fun showPopup(button: Button, username: String) {
    val userProfilePopup = UserProfilePopup(this)
    userProfilePopup.loadUserProfile(urlParser.parse(username) as RedditUserLink)
    userProfilePopup.showWithAnchor(button)
  }

  @OnClick(R.id.playground_menupopup)
  internal fun onClickShowMenuPopup(button: Button) {
    val popupMenu = PopupMenu(this, button)
    popupMenu.menu.add("Poop")
    popupMenu.menu.add("Poop")
    popupMenu.menu.add("Poop")
    popupMenu.show()
  }

  companion object {

    fun start(context: Context) {
      context.startActivity(Intent(context, PlaygroundActivity::class.java))
    }
  }
}
