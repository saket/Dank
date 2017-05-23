package me.saket.dank.utils;

import android.content.res.Resources;

import java.util.Collection;
import java.util.Iterator;

import me.saket.dank.R;

/**
 * Utility methods for String manipulation.
 */
public class Strings {

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
