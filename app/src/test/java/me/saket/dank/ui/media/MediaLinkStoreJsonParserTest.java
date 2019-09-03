package me.saket.dank.ui.media;

import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import me.saket.dank.urlparser.ImgurLink;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.utils.AutoValueMoshiAdapterFactory;
import okio.Okio;

import static junit.framework.TestCase.assertEquals;

public class MediaLinkStoreJsonParserTest {

  @Test
  public void foo() throws IOException {
    Moshi moshi = new Moshi.Builder()
        .add(AutoValueMoshiAdapterFactory.create())
        .build();

    MediaHostRepository.MediaLinkStoreJsonParser storeJsonParser = new MediaHostRepository.MediaLinkStoreJsonParser(moshi);

    ImgurLink resolvedLink = ImgurLink.create("", Link.Type.SINGLE_IMAGE, "", "", "");
    String json = storeJsonParser.toJson(resolvedLink);

    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8.name()));
    MediaLink cachedMediaLink = storeJsonParser.fromJson(Okio.buffer(Okio.source(stream)));

    assertEquals(cachedMediaLink, resolvedLink);
  }
}
