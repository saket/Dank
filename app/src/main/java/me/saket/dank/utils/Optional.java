/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package me.saket.dank.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.NoSuchElementException;
import java.util.Objects;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;

/**
 * Copied from {@link java.util.Optional} because Java 8.
 * <p>
 * A container object which may or may not contain a non-null value.
 * If a value is present, {@code isPresent()} will return {@code true} and
 * {@code get()} will return the value.
 */
@AutoValue
public abstract class Optional<T> {

  private static Optional<?> EMPTY;

  /**
   * If non-null, the value; if null, indicates no value is present
   */
  @Nullable
  abstract T value();

  /**
   * Returns an empty {@code Optional} instance.  No value is present for this
   * Optional.
   *
   * @param <T> Type of the non-existent value
   * @return an empty {@code Optional}
   * @apiNote Though it may be tempting to do so, avoid testing if an object
   * is empty by comparing with {@code ==} against instances returned by
   * {@code Option.empty()}. There is no guarantee that it is a singleton.
   * Instead, use {@link #isPresent()}.
   */
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> empty() {
    if (EMPTY == null) {
      EMPTY = new AutoValue_Optional<>(null);
    }
    return (Optional<T>) EMPTY;
  }

  /**
   * Returns an {@code Optional} with the specified present non-null value.
   *
   * @param <T>   the class of the value
   * @param value the value to be present, which must be non-null
   * @return an {@code Optional} with the value present
   * @throws NullPointerException if value is null
   */
  public static <T> Optional<T> of(T value) {
    return new AutoValue_Optional<>(Objects.requireNonNull(value));
  }

  /**
   * Returns an {@code Optional} describing the specified value, if non-null,
   * otherwise returns an empty {@code Optional}.
   *
   * @param <T>   the class of the value
   * @param value the possibly-null value to describe
   * @return an {@code Optional} with a present value if the specified value
   * is non-null, otherwise an empty {@code Optional}
   */
  public static <T> Optional<T> ofNullable(T value) {
    return value == null ? empty() : of(value);
  }

  /**
   * If a value is present in this {@code Optional}, returns the value,
   * otherwise throws {@code NoSuchElementException}.
   *
   * @return the non-null value held by this {@code Optional}
   * @throws NoSuchElementException if there is no value present
   * @see Optional#isPresent()
   */
  @NonNull
  public T get() {
    if (isPresent()) {
      //noinspection ConstantConditions
      return value();
    }
    throw new NoSuchElementException("No value present");
  }

  /**
   * Return {@code true} if there is a value present, otherwise {@code false}.
   *
   * @return {@code true} if there is a value present, otherwise {@code false}
   */
  public boolean isPresent() {
    return value() != null;
  }

  /**
   * If a value is present, invoke the specified consumer with the value,
   * otherwise do nothing.
   *
   * @param consumer block to be executed if a value is present
   * @throws NullPointerException if value is present and {@code consumer} is
   *                              null
   */
  public void ifPresent(Consumer<? super T> consumer) {
    if (isPresent())
      try {
        consumer.accept(value());
      } catch (Exception e) {
        throw Exceptions.propagate(e);
      }
  }
}
