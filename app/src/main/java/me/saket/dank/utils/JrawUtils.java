package me.saket.dank.utils;

import net.dean.jraw.models.Thing;
import net.dean.jraw.models.attr.Created;

public class JrawUtils {

    public static <T extends Thing & Created> long createdTimeUtc(T thing) {
        return thing.getDataNode().get("created_utc").longValue() * 1000;
    }

}
