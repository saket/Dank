package me.saket.dank.utils;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import net.dean.jraw.models.Account;

/**
 * Moshi JSON adapter for {@link Account}.
 */
public class MoshiAccountAdapter {

  private JacksonHelper jacksonHelper;

  public MoshiAccountAdapter(JacksonHelper jacksonHelper) {
    this.jacksonHelper = jacksonHelper;
  }

  @FromJson
  Account accountFromJson(String accountJson) {
    return new Account(jacksonHelper.parseJsonNode(accountJson));
  }

  @ToJson
  String accountToJson(Account account) {
    return jacksonHelper.toJson(account.getDataNode());
  }
}
