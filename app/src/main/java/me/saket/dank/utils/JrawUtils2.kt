package me.saket.dank.utils

import android.content.res.Resources
import me.saket.dank.R
import me.saket.dank.urlparser.RedditHostedVideoDashPlaylist
import net.dean.jraw.JrawUtils
import net.dean.jraw.models.Created
import net.dean.jraw.models.Message
import net.dean.jraw.models.MoreChildren
import net.dean.jraw.models.Submission
import net.dean.jraw.tree.CommentNode
import timber.log.Timber

/**
 * 2 because [JrawUtils] already exists.
 */
object JrawUtils2 {

  @Deprecated(message = "Inline instead", replaceWith = ReplaceWith("comment.created.time"))
  fun createdTimeUtc(comment: Created): Long {
    return comment.created.time
  }

  fun isThreadContinuation(commentNode: CommentNode<*>): Boolean {
    return commentNode is MoreChildren && (commentNode as MoreChildren).isThreadContinuation
  }

  fun messageReplies(message: Message): List<Message>? {
    return if (message.replies != null) {
      message.replies
    } else {
      emptyList()
    }
  }

  fun secondPartyName(resources: Resources, message: Message, loggedInUserName: String): String? {
    val destination = message.dest

    return if (destination.startsWith("#")) {
      resources.getString(R.string.subreddit_name_r_prefix, message.subreddit)

    } else if (destination.equals(loggedInUserName, ignoreCase = true)) {
      if (message.author == null)
        resources.getString(R.string.subreddit_name_r_prefix, message.subreddit)
      else
        message.author

    } else {
      destination
    }
  }

  fun redditVideoDashPlaylistUrl(submission: Submission): Optional<RedditHostedVideoDashPlaylist> {
    val playlistUrl: String
    val videoWithoutAudioUrl: String

    val embeddedMedia = submission.embeddedMedia
    if (embeddedMedia != null && embeddedMedia.redditVideo != null) {
      val redditVideo = embeddedMedia.redditVideo
      playlistUrl = redditVideo!!.dashUrl
      videoWithoutAudioUrl = redditVideo.fallbackUrl
      return Optional.of(RedditHostedVideoDashPlaylist.create(playlistUrl, videoWithoutAudioUrl))

    } else {
      val crosspostParents = submission.crosspostParents
      if (crosspostParents != null) {
        if (crosspostParents.size > 1) {
          Timber.e(AssertionError("Submission has multiple crosspost parents: ${submission.permalink}"))
        }

        val rootCrossParent = crosspostParents.last()
        val crossPostedRedditVideo = rootCrossParent.embeddedMedia!!.redditVideo
        playlistUrl = crossPostedRedditVideo!!.dashUrl
        videoWithoutAudioUrl = crossPostedRedditVideo.fallbackUrl
        return Optional.of(RedditHostedVideoDashPlaylist.create(playlistUrl, videoWithoutAudioUrl))

      } else {
        return Optional.empty()
      }
    }
  }
}
