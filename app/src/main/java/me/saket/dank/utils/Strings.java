package me.saket.dank.utils;

import android.content.res.Resources;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;

import me.saket.dank.R;

/**
 * Utility methods for String manipulation.
 */
public class Strings {

  private static final DecimalFormat THOUSANDS_FORMATTER = new DecimalFormat("#.#k");
  private static final DecimalFormat MILLIONS_FORMATTER = new DecimalFormat("#.#m");

  public static String concatenateWithCommaAndAnd(Resources resources, Collection<String> strings) {
    final StringBuilder stringBuilder = new StringBuilder(strings.size());
    final Iterator<String> iterator = strings.iterator();
    String comma = resources.getString(R.string.comma_separator_for_multiple_words);
    String and = resources.getString(R.string.and_suffix_for_comma_separated_words);
    int index = 0;

    stringBuilder.append(iterator.next());
    index++;

    while (iterator.hasNext()) {
      if (index == strings.size() - 1) {
        stringBuilder.append(" ").append(and).append(" ");
      } else {
        stringBuilder.append(comma).append(" ");
      }
      stringBuilder.append(iterator.next());

      index++;
    }

    return stringBuilder.toString();
  }

  /**
   * Converts:
   * <li>893       -> 893</li>
   * <li>8933      -> 8.9k</li>
   * <li>89331     -> 89.3k</li>
   * <li>8_933_100 -> 8.9m</li>
   */
  // TODO: Move to another utility class.
  public static String abbreviateScore(float score) {
    if (score % 1 != 0) {
      throw new UnsupportedOperationException("Decimals weren't planned to be supported");
    }

    if (score < 1_000) {
      return String.valueOf((int) score);
    } else if (score < 1_000_000) {
      return THOUSANDS_FORMATTER.format(score / 1_000);
    } else {
      return MILLIONS_FORMATTER.format(score / 1_000_000f);
    }
  }

  // FIXME: rename to ellipsize().
  public static String substringWithBounds(String string, int endIndex) {
    int bound = Math.min(string.length(), endIndex);
    String substringWithBounds = string.substring(0, bound);
    if (string.length() > bound) {
      substringWithBounds += "...";
    }
    return substringWithBounds;
  }

  public static int firstNonWhitespaceCharacterIndex(String string, int startIndex) {
    int length = string.length();
    while (startIndex < length && Character.isWhitespace(string.charAt(startIndex))) {
      startIndex += 1;
    }
    return startIndex;
  }
}
