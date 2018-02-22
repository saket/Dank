package me.saket.dank.data;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Thing;

/**
 * Most of the actionable APIs exposed by JRAW like voting, marking-as-read, etc. only accept
 * {@link Thing} objects instead of their IDs even when only their IDs are supplied for
 * making the API calls to Reddit. This wrapper exists to workaround this problem.
 * <p>
 * Calling any method other than {@link #getFullName()} will result in an NPE.
 */
@AutoValue
public abstract class ThingFullNameWrapper extends StubThing implements Parcelable {

  public abstract String fullName();

  public static ThingFullNameWrapper create(String fullName) {
    return new AutoValue_ThingFullNameWrapper(fullName);
  }

  @Override
  public String getFullName() {
    return fullName();
  }
}
