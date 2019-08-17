package me.saket.dank.ui.preferences;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import io.reactivex.exceptions.Exceptions;

@AutoValue
public abstract class TypefaceResource {

  public static final TypefaceResource DEFAULT = TypefaceResource.create(
      "Roboto regular",
      -1,
      "roboto_regular.ttf");

  public abstract String name();

  @FontRes
  @RequiresApi(Build.VERSION_CODES.O)
  public abstract int id();

  /**
   * Used below Oreo.
   */
  public abstract String compatFileName();

  public Typeface get(Resources resources) {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//      return resources.getFont(id());
//    } else {
    return Typeface.createFromAsset(resources.getAssets(), compatFileName());
//    }
  }

  public static TypefaceResource create(String name, @FontRes int typefaceRes, String compatFileName) {
    return new AutoValue_TypefaceResource(name, typefaceRes, compatFileName);
  }

  public static TypefaceResource create(String name, String compatFileName) {
    return new AutoValue_TypefaceResource(name, -1, compatFileName);
  }

  public static JsonAdapter<TypefaceResource> jsonAdapter(Moshi moshi) {
    return new AutoValue_TypefaceResource.MoshiJsonAdapter(moshi);
  }

  public static class Converter implements Preference.Converter<TypefaceResource> {
    private final Moshi moshi;
    private JsonAdapter<TypefaceResource> adapter;

    public Converter(Moshi moshi) {
      this.moshi = moshi;
    }

    @NonNull
    @Override
    public TypefaceResource deserialize(@NonNull String serialized) {
      JsonAdapter<TypefaceResource> adapter = adapter();
      try {
        //noinspection ConstantConditions
        return adapter.fromJson(serialized);
      } catch (IOException e) {
        throw Exceptions.propagate(e);
      }
    }

    @NonNull
    @Override
    public String serialize(@NonNull TypefaceResource value) {
      JsonAdapter<TypefaceResource> adapter = adapter();
      //noinspection ConstantConditions
      return adapter.toJson(value);
    }

    private JsonAdapter<TypefaceResource> adapter() {
      if (adapter == null) {
        adapter = moshi.adapter(TypefaceResource.class);
      }
      return adapter;
    }
  }
}
