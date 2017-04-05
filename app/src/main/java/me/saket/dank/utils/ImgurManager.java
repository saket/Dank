package me.saket.dank.utils;

import static rx.Single.just;

import com.squareup.moshi.JsonDataException;

import me.saket.dank.data.ImgurAlbumResponse;
import me.saket.dank.data.ImgurResponse;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.exceptions.InvalidImgurAlbumException;
import me.saket.dank.di.Dank;
import rx.Single;
import rx.exceptions.OnErrorThrowable;

public class ImgurManager {

    // TODO: Cache.
    public Single<ImgurResponse> albumCoverImage(MediaLink.ImgurAlbum imgurAlbumLink) {
        return Dank.api()
                .imgurAlbumFree(imgurAlbumLink.albumId())
                .map(albumResponse -> (ImgurResponse) albumResponse)
                .onErrorResumeNext(error -> {
                    if (error instanceof JsonDataException && error.getMessage().contains("was BEGIN_ARRAY at path $.data")) {
                        // This stupid free API sends a different data-type when a single image album is fetched.
                        return Dank.api().imgurImagePaid(imgurAlbumLink.albumId() /* turns out, it's an imageId */);

                    } else {
                        // Accept failures from the free API and retry with the paid API.
                        return just(ImgurAlbumResponse.createEmpty());
                    }
                })
                .flatMap(albumResponse -> albumResponse.hasImages() ? just(albumResponse) : Dank.api().imgurAlbumPaid(imgurAlbumLink.albumId()))
                .doOnSuccess(albumResponse -> {
                    if (!albumResponse.hasImages()) {
                        throw OnErrorThrowable.from(new InvalidImgurAlbumException());
                    }
                });
    }

}
