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

import com.google.auto.value.AutoValue;

/**
 * Container to ease passing around a group of three objects.
 */
@AutoValue
public abstract class Trio<F, S, T> {

  public abstract F first();

  public abstract S second();

  public abstract T third();

  public static <A, B, C> Trio<A, B, C> create(A first, B second, C third) {
    return new AutoValue_Trio<>(first, second, third);
  }

  public static <A, B, C> Trio<A, B, C> create(Pair<A, B> pair, C third) {
    return new AutoValue_Trio<>(pair.first(), pair.second(), third);
  }
}
