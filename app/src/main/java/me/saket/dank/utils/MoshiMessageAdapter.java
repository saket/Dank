package me.saket.dank.utils;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import net.dean.jraw.models.Message;

/**
 * Moshi adapter for {@link Message}.
 */
public class MoshiMessageAdapter {

  private JacksonHelper jacksonHelper;

  public MoshiMessageAdapter(JacksonHelper jacksonHelper) {
    this.jacksonHelper = jacksonHelper;
  }

  @FromJson
  Message messageFromJson(String messageJson) {
    return JrawUtils.parseMessageJson(messageJson, jacksonHelper);
  }

  @ToJson
  String messageToJson(Message message) {
    return jacksonHelper.toJson(message);
  }

}
