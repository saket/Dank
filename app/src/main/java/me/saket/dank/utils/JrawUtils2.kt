package me.saket.dank.utils

import android.content.res.Resources
import me.saket.dank.R
import me.saket.dank.urlparser.RedditHostedVideoDashPlaylist
import net.dean.jraw.JrawUtils
import net.dean.jraw.models.Created
import net.dean.jraw.models.Identifiable
import net.dean.jraw.models.Message
import net.dean.jraw.models.Submission
import net.dean.jraw.tree.CommentNode
import timber.log.Timber

/**
 * 2 because [JrawUtils] already exists.
 */
object JrawUtils2 {

  @JvmStatic
  fun <T> generateAdapterId(thing: T): Long where T : Identifiable, T : Created {
    return thing.fullName.hashCode() + thing.created.time
  }

  @JvmStatic
  fun isThreadContinuation(commentNode: CommentNode<*>): Boolean {
    return commentNode.moreChildren?.isThreadContinuation ?: false
  }

  @JvmStatic
  fun messageReplies(message: Message): List<Message> {
    return if (message.replies != null) {
      message.replies!!
    } else {
      emptyList()
    }
  }

  @JvmStatic
  fun secondPartyName(resources: Resources, message: Message, loggedInUserName: String): String? {
    val destination = message.dest

    return when {
      destination.startsWith("#") -> {
        resources.getString(R.string.subreddit_name_r_prefix, message.subreddit)
      }
      destination.equals(loggedInUserName, ignoreCase = true) -> {
        message.author ?: resources.getString(R.string.subreddit_name_r_prefix, message.subreddit)
      }
      else -> destination
    }
  }

  @JvmStatic
  fun redditVideoDashPlaylistUrl(submission: Submission): Optional<RedditHostedVideoDashPlaylist> {
    val playlistUrl: String
    val videoWithoutAudioUrl: String

    val embeddedMedia = submission.embeddedMedia
    return if (embeddedMedia != null && embeddedMedia.redditVideo != null) {
      val redditVideo = embeddedMedia.redditVideo
      playlistUrl = redditVideo!!.dashUrl
      videoWithoutAudioUrl = redditVideo.fallbackUrl
      Optional.of(RedditHostedVideoDashPlaylist.create(playlistUrl, videoWithoutAudioUrl))

    } else {
      val crossPostParents = submission.crosspostParents
      return if (crossPostParents?.isNotEmpty() == true) {
        if (crossPostParents.size > 1) {
          Timber.e(AssertionError("Submission has multiple crosspost parents: ${submission.permalink}"))
        }

        val rootCrossParent = crossPostParents.last()
        val crossPostedRedditVideo = rootCrossParent.embeddedMedia!!.redditVideo
        playlistUrl = crossPostedRedditVideo!!.dashUrl
        videoWithoutAudioUrl = crossPostedRedditVideo.fallbackUrl
        Optional.of(RedditHostedVideoDashPlaylist.create(playlistUrl, videoWithoutAudioUrl))

      } else {
        Optional.empty()
      }
    }
  }
}
