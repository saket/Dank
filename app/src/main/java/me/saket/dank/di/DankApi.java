package me.saket.dank.di;

import me.saket.dank.data.StreamableVideoResponse;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Single;

public interface DankApi {

    @GET("https://api.streamable.com/videos/{videoId}")
    Single<StreamableVideoResponse> streamableVideoDetails(
            @Path("videoId") String videoId
    );

}
