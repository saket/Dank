package me.saket.dank.ui.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.saket.dank.R
import me.saket.dank.ui.DankActivity

class CreateNewPostActivity : DankActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create_new_post)
  }

  companion object {

    @JvmStatic
    fun intent(context: Context): Intent {
      return Intent(context, CreateNewPostActivity::class.java)
    }
  }
}
