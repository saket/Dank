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

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.NoSuchElementException;
import java.util.Objects;
import javax.inject.Provider;

import io.reactivex.Observable;
import io.reactivex.ObservableConverter;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

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
   * Use with {@link Observable#as(ObservableConverter)}.
   * Convenience for Observable#map(item -> Optional.of(item)).
   */
  public static <T> ObservableConverter<T, Observable<Optional<T>>> of() {
    return upstream -> upstream.map(item -> Optional.of(item));
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
  public static <T> Optional<T> ofNullable(@Nullable T value) {
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

  public boolean isEmpty() {
    return value() == null;
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

  /**
   * Return the value if present, otherwise return {@code other}.
   *
   * @param other the value to be returned if there is no value present, may be null
   * @return the value, if present, otherwise {@code other}
   */
  public T orElse(@Nullable T other) {
    return value() != null ? value() : other;
  }

  /**
   * Return the contained value, if present, otherwise throw an exception
   * to be created by the provided supplier.
   *
   * @apiNote A method reference to the exception constructor with an empty
   * argument list can be used as the supplier. For example,
   * {@code IllegalStateException::new}
   *
   * @param <X> Type of the exception to be thrown
   * @param exceptionSupplier The supplier which will return the exception to
   * be thrown
   * @return the present value
   * @throws X if there is no value present
   * @throws NullPointerException if no value is present and
   * {@code exceptionSupplier} is null
   */
  public <X extends Throwable> T orElseThrow(Provider<? extends X> exceptionSupplier) throws X {
    if (value() != null) {
      return value();
    } else {
      throw exceptionSupplier.get();
    }
  }

  /**
   * If a value is present, apply the provided mapping function to it,
   * and if the result is non-null, return an {@code Optional} describing the
   * result.  Otherwise return an empty {@code Optional}.
   *
   * @param <U>    The type of the result of the mapping function.
   * @param mapper a mapping function to apply to the value, if present.
   * @return an {@code Optional} describing the result of applying a mapping
   * function to the value of this {@code Optional}, if a value is present,
   * otherwise an empty {@code Optional}.
   * @throws NullPointerException if the mapping function is null.
   * @apiNote This method supports post-processing on optional changeEvents, without
   * the need to explicitly check for a return status.  For example, the
   * following code traverses a stream of file names, selects one that has
   * not yet been processed, and then opens that file, returning an
   * {@code Optional<FileInputStream>}:
   * <p>
   * <pre>{@code
   *     Optional<FileInputStream> fis =
   *         names.stream().filter(name -> !isProcessedYet(name))
   *                       .findFirst()
   *                       .map(name -> new FileInputStream(name));
   * }</pre>
   * <p>
   * Here, {@code findFirst} returns an {@code Optional<String>}, and then
   * {@code map} returns an {@code Optional<FileInputStream>} for the desired
   * file if one exists.
   */
  public <U> Optional<U> map(io.reactivex.functions.Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    if (!isPresent())
      return Optional.empty();
    else {
      try {
        return Optional.ofNullable(mapper.apply(value()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * If a value is present, apply the provided {@code Optional}-bearing
   * mapping function to it, return that result, otherwise return an empty
   * {@code Optional}.  This method is similar to {@link #map(Function)},
   * but the provided mapper is one whose result is already an {@code Optional},
   * and if invoked, {@code flatMap} does not wrap it with an additional
   * {@code Optional}.
   *
   * @param <U> The type parameter to the {@code Optional} returned by
   * @param mapper a mapping function to apply to the value, if present
   *           the mapping function
   * @return the result of applying an {@code Optional}-bearing mapping
   * function to the value of this {@code Optional}, if a value is present,
   * otherwise an empty {@code Optional}
   * @throws NullPointerException if the mapping function is null or returns
   * a null result
   */
  public<U> Optional<U> flatMap(io.reactivex.functions.Function<? super T, Optional<U>> mapper) {
    Objects.requireNonNull(mapper);
    if (!isPresent())
      return empty();
    else {
      try {
        return Objects.requireNonNull(mapper.apply(value()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  /**
   * Indicates whether some other object is "equal to" this Optional. The
   * other object is considered equal if:
   * <ul>
   * <li>it is also an {@code Optional} and;
   * <li>both instances have no value present or;
   * <li>the present values are "equal to" each other via {@code equals()}.
   * </ul>
   *
   * @param obj an object to be tested for equality
   * @return {code true} if the other object is "equal to" this object
   * otherwise {@code false}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Optional)) {
      return false;
    }

    Optional<?> other = (Optional<?>) obj;
    return Objects.equals(value(), other.value());
  }

  /**
   * Returns the hash code value of the present value, if any, or 0 (zero) if
   * no value is present.
   *
   * @return hash code value of the present value or 0 if no value is present
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(value());
  }

  /**
   * Returns a non-empty string representation of this Optional suitable for
   * debugging. The exact presentation format is unspecified and may vary
   * between implementations and versions.
   *
   * @implSpec If a value is present the result must include its string
   * representation in the result. Empty and present Optionals must be
   * unambiguously differentiable.
   *
   * @return the string representation of this instance
   */
  @Override
  public String toString() {
    return value() != null
        ? String.format("Optional[%s]", value())
        : "Optional.empty";
  }
}
