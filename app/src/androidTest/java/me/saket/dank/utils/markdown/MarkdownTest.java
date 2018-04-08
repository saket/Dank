package me.saket.dank.utils.markdown;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import me.saket.dank.utils.SafeFunction;

@RunWith(AndroidJUnit4.class)
public class MarkdownTest {

  @Test
  public void spoiler() {
    SafeFunction<String, String> markdownToHtmlParser = MarkdownModule.markdownToHtmlParser();
    String html = markdownToHtmlParser.apply("[spoiler](/s \"hidden text\"");
    System.out.println(html);
  }
}
