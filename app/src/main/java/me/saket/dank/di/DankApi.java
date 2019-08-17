package me.saket.dank.di;

import androidx.annotation.CheckResult;

import io.reactivex.Single;
import me.saket.dank.data.StreamableVideoResponse;
import me.saket.dank.data.UnfurlLinkResponse;
import me.saket.dank.ui.giphy.GiphySearchResponse;
import me.saket.dank.ui.media.ImgurAlbumResponse;
import me.saket.dank.ui.media.ImgurImageResponse;
import me.saket.dank.ui.media.ImgurUploadResponse;
import me.saket.dank.ui.media.gfycat.GfycatOauthResponse;
import me.saket.dank.ui.media.gfycat.GfycatResponse;
import okhttp3.MultipartBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DankApi {

  String HEADER_IMGUR_AUTH = "Authorization: Client-ID 87450e5590435e9";
  String HEADER_MASHAPE_KEY = "X-Mashape-Key: VOjpM0pXeAmshuRGE4Hhe6KY9Ouep1YCLx8jsnaivCFNYALpN5";
  String HEADER_WHOLESOME_API_AUTH = "Authorization";
  String WHOLESOME_API_HOST = "dank-wholesome.herokuapp.com";
  String GIPHY_API_KEY = "SFGHZ6SYGn3AzZ07b2tNpENCEDdYTzpB";

// ======== IMGUR ======== //

  /**
   * Get images in an Imgur album. This is a paid API so we try to minimize its usage. The response
   * is wrapped in {@link Response} so that the headers can be extracted for checking Imgur rate-limits.
   */
  @CheckResult
  @GET("https://imgur-apiv3.p.mashape.com/3/album/{albumId}")
  @Headers({ HEADER_IMGUR_AUTH, HEADER_MASHAPE_KEY })
  Single<Response<ImgurAlbumResponse>> imgurAlbum(
      @Path("albumId") String albumId
  );

  /**
   * Get an image's details from Imgur. This is also a paid API.
   */
  @CheckResult
  @GET("https://imgur-apiv3.p.mashape.com/3/image/{imageId}")
  @Headers({ HEADER_IMGUR_AUTH, HEADER_MASHAPE_KEY })
  Single<Response<ImgurImageResponse>> imgurImage(
      @Path("imageId") String imageId
  );

  @CheckResult
  @Multipart
  @POST("https://imgur-apiv3.p.mashape.com/3/image")
  @Headers({ HEADER_IMGUR_AUTH, HEADER_MASHAPE_KEY })
  Single<Response<ImgurUploadResponse>> uploadToImgur(
      @Part MultipartBody.Part file,
      @Query("type") String fileType
  );

// ======== STREAMABLE ======== //

  /**
   * Get downloadable video URLs from a streamable.com link.
   */
  @CheckResult
  @GET("https://api.streamable.com/videos/{videoId}")
  Single<StreamableVideoResponse> streamableVideoDetails(
      @Path("videoId") String videoId
  );

// ======== WHOLESOME ======== //

  @CheckResult
  @GET("https://" + WHOLESOME_API_HOST + "/unfurl")
  Single<UnfurlLinkResponse> unfurlUrl(
      @Query("url") String url,
      @Query("ignoreSocialMetadata") boolean ignoreSocialMetadata
  );

// ======== GIPHY ======== //

  @CheckResult
  @GET("https://api.giphy.com/v1/gifs/search")
  Single<GiphySearchResponse> giphySearch(
      @Query("api_key") String apiKey,
      @Query("q") String searchQuery,
      @Query("limit") int itemsPerPage,
      @Query("offset") int paginationOffset
  );

  @CheckResult
  @GET("https://api.giphy.com/v1/gifs/trending")
  Single<GiphySearchResponse> giphyTrending(
      @Query("api_key") String apiKey,
      @Query("limit") int itemsPerPage,
      @Query("offset") int paginationOffset
  );

// ======== GFYCAT ======== //

  @CheckResult
  @GET("https://api.gfycat.com/v1/oauth/token?grant_type=client_credentials")
  Single<GfycatOauthResponse> gfycatOAuth(
      @Query("client_id") String clientId,
      @Query("client_secret") String clientSecret
  );

  @GET("https://api.gfycat.com/v1/gfycats/{gfyid}")
  Single<GfycatResponse> gfycat(
      @Path("gfyid") String threeWordId
  );

  @GET("https://api.gfycat.com/v1/gfycats/{gfyid}")
  Single<GfycatResponse> gfycat(
      @Header("Authorization") String authHeader,
      @Path("gfyid") String threeWordId
  );
}
