package me.saket.dank.di;

import io.reactivex.Single;
import me.saket.dank.data.ImgurAlbumResponse;
import me.saket.dank.data.ImgurImageResponse;
import me.saket.dank.data.StreamableVideoResponse;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface DankApi {

  String HEADER_MASHAPE_KEY = "X-Mashape-Key: VOjpM0pXeAmshuRGE4Hhe6KY9Ouep1YCLx8jsnaivCFNYALpN5";
  String HEADER_IMGUR_AUTH = "Authorization: Client-ID 87450e5590435e9";

  /**
   * Get images in an Imgur album. This is a paid API so we try to minimize its usage. The response
   * is wrapped in {@link Response} so that the headers can be extracted for checking Imgur rate-limits.
   */
  @GET("https://imgur-apiv3.p.mashape.com/3/album/{albumId}")
  @Headers({ HEADER_IMGUR_AUTH, HEADER_MASHAPE_KEY })
  Single<Response<ImgurAlbumResponse>> imgurAlbumPaid(
      @Path("albumId") String albumId
  );

  /**
   * Get an image's details from Imgur. This is also a paid API.
   */
  @GET("https://imgur-apiv3.p.mashape.com/3/gallery/image/{imageId}")
  @Headers({ HEADER_IMGUR_AUTH, HEADER_MASHAPE_KEY })
  Single<Response<ImgurImageResponse>> imgurImagePaid(
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
