package me.saket.dank.utils;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import net.dean.jraw.models.LoggedInAccount;

public class MoshiLoggedInAccountAdapter {

  private JacksonHelper jacksonHelper;

  public MoshiLoggedInAccountAdapter(JacksonHelper jacksonHelper) {
    this.jacksonHelper = jacksonHelper;
  }

  @FromJson
  LoggedInAccount accountFromJson(String accountJson) {
    return new LoggedInAccount(jacksonHelper.parseJsonNode(accountJson));
  }

  @ToJson
  String accountToJson(LoggedInAccount account) {
    return jacksonHelper.toJson(account.getDataNode());
  }
}
