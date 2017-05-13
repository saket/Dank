package me.saket.dank.utils;

import android.content.res.Resources;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import me.saket.dank.R;

/**
 * Utility methods for String manipulation.
 */
public class Strings {

  /**
   * "A, B, C and D".
   */
  public static String concatenateWithCommaAndAnd2(Resources resources, List<String> strings) {
    final StringBuilder stringBuilder = new StringBuilder(strings.size());
    String comma = resources.getString(R.string.comma_separator_for_multiple_words);
    String and = resources.getString(R.string.and_suffix_for_comma_separated_words);

    stringBuilder.append(strings.get(0));
    for (int index = 1; index < strings.size(); index++) {
      if (index == strings.size() - 1) {
        stringBuilder.append(" ").append(and).append(" ");
      } else {
        stringBuilder.append(comma).append(" ");
      }
      stringBuilder.append(strings.get(index));
    }

    return stringBuilder.toString();
  }

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

}
