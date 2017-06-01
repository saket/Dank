package me.saket.dank.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import net.dean.jraw.models.Submission;

/**
 * Moshi adapter for {@link Submission}.
 */
public class MoshiSubmissionAdapter {

  private JacksonHelper jacksonHelper;

  public MoshiSubmissionAdapter(JacksonHelper jacksonHelper) {
    this.jacksonHelper = jacksonHelper;
  }

  @FromJson
  Submission messageFromJson(String messageJson) {
    JsonNode jsonNode = jacksonHelper.parseJsonNode(messageJson);
    return new Submission(jsonNode);
  }

  @ToJson
  String messageToJson(Submission message) {
    return jacksonHelper.toJson(message);
  }

}
