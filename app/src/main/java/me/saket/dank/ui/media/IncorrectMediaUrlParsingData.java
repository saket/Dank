package me.saket.dank.ui.media;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.urlparser.Link;
import me.saket.dank.utils.RxHashSet;

public class IncorrectMediaUrlParsingData {

  // TODO: store this to persistence.
  private static RxHashSet<Link> flaggedLinks = new RxHashSet<>();

  @Inject
  public IncorrectMediaUrlParsingData() {
  }

  public Completable flag(Link link) {
    return Completable.fromAction(() -> flaggedLinks.add(link));
  }

  public Observable<Boolean> isFlagged(Link link) {
    return flaggedLinks.changes()
        .map(o -> flaggedLinks.contains(link));
  }

  public void clear() {
    flaggedLinks.clear();
  }
}
