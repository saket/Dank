package me.saket.dank.urlparser;

/**
 * Implemented by {@link Link} classes whose actual data is unknown. For an Imgur album, this will be true
 * when only the album URL is known and the title, description, cover images, etc. will have to be looked up.
 * For Streamable, this will true when direct links to the video needs to be looked up.
 */
public interface UnresolvedMediaLink {
}
