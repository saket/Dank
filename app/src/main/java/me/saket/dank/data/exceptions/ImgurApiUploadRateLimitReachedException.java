package me.saket.dank.data.exceptions;

/**
 * Thrown when Dank runs out Imgur rate limits, in order to avoid over-billing.
 */
public class ImgurApiUploadRateLimitReachedException extends RuntimeException {
}
