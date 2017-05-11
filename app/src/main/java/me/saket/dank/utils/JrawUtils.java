package me.saket.dank.utils;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.attr.Created;

public class JrawUtils {

  /**
   * Prefer to use this over {@link Contribution#getCreated()} to avoid unnecessary creation of a Date object.
   */
  public static <T extends Thing & Created> long createdTimeUtc(T thing) {
    return thing.getDataNode().get("created_utc").longValue() * 1000;
  }

  public static Message parseMessageJson(String json, JacksonHelper jacksonHelper) {
    JsonNode jsonNode = jacksonHelper.fromJson(json);
    //noinspection ConstantConditions
    boolean isCommentMessage = jsonNode.get("was_comment").asBoolean();
    return isCommentMessage ? new CommentMessage(jsonNode) : new PrivateMessage(jsonNode);
  }

  public static String getMessageBodyHtml(Message message) {
    return message.getDataNode().get("body_html").asText();
  }
}
