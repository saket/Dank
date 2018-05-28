package me.saket.dank.reddit

import dagger.Module
import dagger.Provides
import me.saket.dank.reddit.jraw.JrawReddit
import me.saket.dank.reddit.jraw.JrawRedditModule
import javax.inject.Singleton

@Module(includes = [JrawRedditModule::class])
class RedditModule {

  @Provides
  @Singleton
  fun reddit(jrawReddit: JrawReddit): Reddit {
    return jrawReddit
  }
}
