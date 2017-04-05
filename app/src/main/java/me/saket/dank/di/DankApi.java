package me.saket.dank.di;

import me.saket.dank.data.ImgurAlbumResponse;
import me.saket.dank.data.ImgurImageResponse;
import me.saket.dank.data.StreamableVideoResponse;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import rx.Single;

public interface DankApi {

    String HEADER_AUTH = "Authorization: Client-ID 87450e5590435e9";

    /**
     * Get images in an Imgur album using a less-known public API that is available for free.
     * Note: this API seems to always return a 200, even when an invalid album ID is used.
     */
    @GET("https://imgur.com/ajaxalbums/getimages/{albumId}/hit.json?all=true")
    Single<ImgurAlbumResponse> imgurAlbumFree(
            @Path("albumId") String albumId
    );

    /**
     * Get images in an Imgur album. This is a paid API so we try to minimize its usage.
     */
    @GET("https://api.imgur.com/3/album/{albumId}")
    @Headers(HEADER_AUTH)
    Single<ImgurAlbumResponse> imgurAlbumPaid(
            @Path("albumId") String albumId
    );

    /**
     * Get an image's details from Imgur. This is also a paid API.
     */
    @GET("https://api.imgur.com/3/gallery/image/{imageId}")
    @Headers(HEADER_AUTH)
    Single<ImgurImageResponse> imgurImagePaid(
            @Path("imageId") String imageId
    );

    /**
     * Get downloadable video URLs from a streamable.com link.
     */
    @GET("https://api.streamable.com/videos/{videoId}")
    Single<StreamableVideoResponse> streamableVideoDetails(
            @Path("videoId") String videoId
    );

}
