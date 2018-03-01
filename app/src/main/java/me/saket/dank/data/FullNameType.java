package me.saket.dank.data;

public enum FullNameType {
  COMMENT("t1_"),
  ACCOUNT("t2_"),
  SUBMISSION("t3_"),
  MESSAGE("t4_"),
  SUBREDDIT("t5_"),
  AWARD("t6_"),
  UNKNOWN("poop");

  private final String prefix;

  FullNameType(String prefix) {
    this.prefix = prefix;
  }

  public static FullNameType parse(String fullName) {
    for (FullNameType type : FullNameType.values()) {
      if (fullName.startsWith(type.prefix)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public String prefix() {
    return prefix;
  }
}
