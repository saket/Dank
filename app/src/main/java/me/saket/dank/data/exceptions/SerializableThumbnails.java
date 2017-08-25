package me.saket.dank.data.exceptions;

import android.os.Bundle;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.models.Thumbnails;

import java.io.Serializable;

/**
 * Workaround to pass {@link Thumbnails} in {@link Bundle bundles}.
 */
public class SerializableThumbnails extends Thumbnails implements Serializable {

  public SerializableThumbnails(JsonNode dataNode) {
    super(dataNode);
  }
}
