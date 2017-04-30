package me.saket.dank.utils;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import timber.log.Timber;

/**
 * Utility methods for dealing with JSON using Jackson.
 */
public class JacksonUtils {

    public static String toJson(JsonNode jsonNode) {
        return jsonNode.toString();
    }

    @Nullable
    public static JsonNode fromJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (IOException e) {
            Timber.e(e, "Couldn't deserialize json: %s", json);
            return null;
        }
    }

}
