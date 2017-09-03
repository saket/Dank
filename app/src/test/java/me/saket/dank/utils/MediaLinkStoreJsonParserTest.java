package me.saket.dank.utils;

import static junit.framework.TestCase.assertEquals;

import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.MediaLink;
import okio.Okio;

public class MediaLinkStoreJsonParserTest {

  @Test
  public void foo() throws IOException {
    Moshi moshi = new Moshi.Builder()
        .add(AutoValueMoshiAdapterFactory.create())
        .build();

    MediaHostRepository.MediaLinkStoreJsonParser storeJsonParser = new MediaHostRepository.MediaLinkStoreJsonParser(moshi);

    ImgurLink resolvedLink = ImgurLink.create("", "", "", "");
    String json = storeJsonParser.toJson(resolvedLink);

    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8.name()));
    MediaLink cachedMediaLink = storeJsonParser.fromJson(Okio.buffer(Okio.source(stream)));

    assertEquals(cachedMediaLink, resolvedLink);
  }
}
