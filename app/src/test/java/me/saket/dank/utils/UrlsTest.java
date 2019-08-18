package me.saket.dank.utils;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Uri.class })
public class UrlsTest {

  @Before
  public void setUp() {
    PowerMockito.mockStatic(Uri.class);

    PowerMockito.when(Uri.parse(anyString())).thenAnswer(invocation -> {
      String url = invocation.getArgument(0);
      return UrlParserTest.createMockUriFor(url);
    });
  }

  @Test
  public void subdomain() {
    assertEquals("v", Urls.subdomain(Uri.parse("https://v.redd.it/fjpqnd127wf01")).get());
    assertEquals("i", Urls.subdomain(Uri.parse("https://i.redd.it/5524cd")).get());
    assertEquals(Optional.empty(), Urls.subdomain(Uri.parse("https://redd.it/5524cd")));
    assertEquals("a.b.c", Urls.subdomain(Uri.parse("https://a.b.c.rredd.it/5524cd")).get());
  }

  @Test
  public void parseFileNameWithExtension() {
    assertEquals("a.jpg", Urls.parseFileNameWithExtension("https://i.imgur.com/a.jpg"));
    assertEquals("b.jpg", Urls.parseFileNameWithExtension("https://i.redd.it/b.jpg"));
    assertEquals("c", Urls.parseFileNameWithExtension("https://i.redd.it/c"));
    assertEquals("d.jpg", Urls.parseFileNameWithExtension("https://i.imgur.com/abc/d.jpg"));
  }

  @Test
  public void parseFileNameWithExtension_redditVideos() {
    assertEquals("xyz.mp4", Urls.parseFileNameWithExtension("https://v.redd.it/xyz/DASHPlaylist.mpd"));
    assertEquals("add.mp4", Urls.parseFileNameWithExtension("https://v.redd.it/gfuigw/add/DASHPlaylist.mpd"));
    assertEquals("DASHPlaylist.mpd", Urls.parseFileNameWithExtension("https://v.redd.it/DASHPlaylist.mpd"));
    assertEquals("DASHPlaylist.mpd", Urls.parseFileNameWithExtension("https://i.imgur.com/gui/DASHPlaylist.mpd"));
  }
}
