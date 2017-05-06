package me.saket.dank.data.exceptions;

/**
 * RxJava doesn't have a scan() operator that can return an Observable, so we cannot stop
 * a stream when we detect that pagination has finished by returning Observable.never().
 * Thus, this exception.
 */
public class PaginationCompleteException extends RuntimeException {
}
