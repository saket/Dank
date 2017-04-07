package me.saket.dank.utils;

import static rx.Single.just;

import me.saket.dank.data.ImgurAlbumResponse;
import me.saket.dank.data.ImgurResponse;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.exceptions.InvalidImgurAlbumException;
import me.saket.dank.di.Dank;
import retrofit2.adapter.rxjava.HttpException;
import rx.Single;
import rx.exceptions.OnErrorThrowable;
import timber.log.Timber;

public class ImgurManager {

    // TODO: Cache.
//    public Single<ImgurResponse> albumCoverImage(MediaLink.UnresolvedImgurGallery unresolvedImgurGalleryLink) {
//        return Dank.api()
//                .imgurAlbumFree(unresolvedImgurGalleryLink.albumId())
//                .map(albumResponse -> (ImgurResponse) albumResponse)
//                .onErrorResumeNext(error -> {
//                    if (error instanceof JsonDataException && error.getMessage().contains("was BEGIN_ARRAY at path $.data")) {
//                        // This stupid free API sends a different data-type when a single image album is fetched.
//                        return Dank.api().imgurImagePaid(unresolvedImgurGalleryLink.albumId() /* turns out, it's an imageId */);
//
//                    } else {
//                        // Accept failures from the free API and retry with the paid API.
//                        return just(ImgurAlbumResponse.createEmpty());
//                    }
//                })
//                .flatMap(albumResponse -> albumResponse.hasImages() ? just(albumResponse) : Dank.api().imgurAlbumPaid(unresolvedImgurGalleryLink.albumId()))
//                .doOnSuccess(albumResponse -> {
//                    if (!albumResponse.hasImages()) {
//                        throw OnErrorThrowable.from(new InvalidImgurAlbumException());
//                    }
//                });
//    }

    public Single<ImgurResponse> gallery(MediaLink.ImgurUnresolvedGallery imgurUnresolvedGalleryLink) {
        return Dank.api()
                .imgurAlbumPaid(imgurUnresolvedGalleryLink.albumId())
                .map(albumResponse -> (ImgurResponse) albumResponse)
                .onErrorResumeNext(error -> {
                    if (error instanceof HttpException) {
                        if (((HttpException) error).code() == 404) {
                            return just(ImgurAlbumResponse.createEmpty());
                        }
                    }
                    throw OnErrorThrowable.from(error);
                })
                .flatMap(albumResponse -> {
                    Timber.i("albumResponse: %s", albumResponse);
                    if (!albumResponse.hasImages()) {
                        // Okay, let's check if it was a single image.
                        return Dank.api().imgurImagePaid(imgurUnresolvedGalleryLink.albumId());
                    } else {
                        return just(albumResponse);
                    }
                })
                .doOnSuccess(albumResponse -> {
                    if (!albumResponse.hasImages()) {
                        throw OnErrorThrowable.from(new InvalidImgurAlbumException());
                    }
                });
    }

}
