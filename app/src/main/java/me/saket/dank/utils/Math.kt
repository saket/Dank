@file:JvmName("Math2")

package me.saket.dank.utils

/** Clamp between a given maximum and minimum. */
fun Float.clamp(min: Float, max: Float) = when {
  this > max -> max
  this < min -> min
  else -> this
}
