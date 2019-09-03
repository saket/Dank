package me.saket.dank.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.internal.Util;

import net.dean.jraw.models.Identifiable;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoshiAdapter {

  private Moshi moshi;

  @Inject
  public MoshiAdapter(Moshi moshi) {
    this.moshi = moshi;
  }

  public <T> JsonAdapter<T> create(Class<T> clazz) {
    return create(Util.canonicalize(clazz));
  }

  public <T> JsonAdapter<T> create(Type type) {
    JsonAdapter<T> adapter = moshi.adapter(type);
    return isRedditModel(type)
        ? adapter.serializeNulls()
        : adapter;
  }

  private boolean isRedditModel(Type type) {
    if (type instanceof GenericArrayType) {
      return Identifiable.class.isAssignableFrom((Class) ((GenericArrayType) type).getGenericComponentType());
    }
    if (type instanceof ParameterizedType) {
      return Identifiable.class.isAssignableFrom((Class) ((ParameterizedType) type).getActualTypeArguments()[0]);
    }
    return type instanceof Class && Identifiable.class.isAssignableFrom(((Class) type));
  }
}
