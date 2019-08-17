/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.saket.dank.utils;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Copied from {@link androidx.core.util.Pair} to remove all @Nullable annotations.
 * <p>
 * Container to ease passing around a tuple of two objects.
 */
public abstract class Pair<F, S> {

  public abstract F first();

  public abstract S second();

  public static <A, B> Pair<A, B> create(A first, B second) {
    return new AutoValue_Pair_NonNullPair<>(first, second);
  }

  public static <A, B> Pair<A, B> createNullable(@Nullable A first, @Nullable B second) {
    return new AutoValue_Pair_NullablePair<>(first, second);
  }

  @AutoValue
  abstract static class NonNullPair<F, S> extends Pair<F, S> {
    public abstract F first();

    public abstract S second();
  }

  @AutoValue
  abstract static class NullablePair<F, S> extends Pair<F, S> {
    @Nullable
    public abstract F first();

    @Nullable
    public abstract S second();
  }
}
