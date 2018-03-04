package me.saket.dank.utils;

import android.content.res.Resources;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.attr.Created;

import java.util.Collections;
import java.util.List;

import me.saket.dank.R;

// TODO: Submit all these methods to JRAW.
public class JrawUtils {

  public static Message parseMessageJson(String json, JacksonHelper jacksonHelper) {
    JsonNode jsonNode = jacksonHelper.parseJsonNode(json);
    //noinspection ConstantConditions
    boolean isCommentMessage = jsonNode.get("was_comment").asBoolean();
    return isCommentMessage ? new CommentMessage(jsonNode) : new PrivateMessage(jsonNode);
  }

  /**
   * Prefer to use this over {@link Contribution#getCreated()} to avoid unnecessary creation of a Date object.
   */
  public static <T extends Thing & Created> long createdTimeUtc(T thing) {
    return thing.getDataNode().get("created_utc").longValue() * 1000;
  }

  public static List<Message> messageReplies(PrivateMessage message) {
    JsonNode repliesNode = message.getDataNode().get("replies");
    return repliesNode.isObject()
        ? new Listing<>(repliesNode.get("data"), Message.class)
        : Collections.emptyList();
  }

  public static String messageBodyHtml(Message message) {
    return message.getDataNode().get("body_html").asText(message.getBody());
  }

  public static String commentBodyHtml(Comment comment) {
    return comment.getDataNode().get("body_html").asText(comment.getBody());
  }

  public static String selfPostHtml(Submission submission) {
    return submission.getDataNode().get("selftext_html").asText(submission.getSelftext() /* defaultValue */);
  }

  public static String secondPartyName(Resources resources, Message message, String loggedInUserName) {
    String destination = message.getDataNode().get("dest").asText();

    if (destination.startsWith("#")) {
      return resources.getString(R.string.subreddit_name_r_prefix, message.getSubreddit());

    } else if (destination.equalsIgnoreCase(loggedInUserName)) {
      return message.getAuthor() == null
          ? resources.getString(R.string.subreddit_name_r_prefix, message.getSubreddit())
          : message.getAuthor();

    } else {
      return destination;
    }
  }

  public static String commentMessageContextUrl(CommentMessage message) {
    return message.getDataNode().get("context").asText();
  }

  public static String permalink(Comment comment) {
    return comment.getDataNode().get("permalink").asText();
  }

  // First: Playlist URL
  // Second: Dash URL without audio.
  public static Pair<String, String> redditVideoDashPlaylistUrl(Submission submission) {
    String playlistUrl;
    String videoWithoutAudioUrl;

    JsonNode redditVideoNode = submission.getDataNode().get("secure_media").get("reddit_video");
    if (redditVideoNode != null) {
      playlistUrl = redditVideoNode.get("dash_url").asText();
      videoWithoutAudioUrl = redditVideoNode.get("fallback_url").asText();

    } else {
      boolean hasCrossParent = submission.getDataNode().get("crosspost_parent_list") != null;
      if (hasCrossParent) {
        JsonNode crossPostParentListNode = submission.getDataNode().get("crosspost_parent_list");
        if (crossPostParentListNode.size() > 1) {
          throw new UnsupportedOperationException("Multiple cross-post parents! " + submission.getPermalink());
        }
        JsonNode crossPostParentVideoNode = crossPostParentListNode.get(0).get("secure_media").get("reddit_video");
        playlistUrl = crossPostParentVideoNode.get("dash_url").asText();
        videoWithoutAudioUrl = crossPostParentVideoNode.get("fallback_url").asText();

      } else {
        throw new UnsupportedOperationException("Couldn't find reddit video URL for sub: " + submission.getPermalink());
      }
    }
    return Pair.create(playlistUrl, videoWithoutAudioUrl);
  }
}
