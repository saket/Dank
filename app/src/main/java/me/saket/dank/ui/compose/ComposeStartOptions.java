package me.saket.dank.ui.compose;

import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

  public abstract String secondPartyName();

  @Nullable
  public abstract String preFilledText();

  public abstract ComposeStartOptions.Builder toBuilder();

  public static ComposeStartOptions create(String secondPartyName, @Nullable String preFilledText) {
    return new AutoValue_ComposeStartOptions(secondPartyName, preFilledText);
  }

  public static ComposeStartOptions create(String secondPartyName) {
    return new AutoValue_ComposeStartOptions(secondPartyName, null);
  }

  public static ComposeStartOptions.Builder builder() {
    return new AutoValue_ComposeStartOptions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder secondPartyName(String secondPartyName);

    public abstract Builder preFilledText(String preFilledText);

    public abstract ComposeStartOptions build();
  }
}
