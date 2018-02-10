package me.saket.dank.ui.submission;

import net.dean.jraw.models.PublicContribution;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import me.saket.dank.utils.RxHashSet;

@Singleton
public class BookmarksRepository {

  private final RxHashSet<String> savedFullNames = new RxHashSet<>();

  @Inject
  public BookmarksRepository() {
  }

  public void markAsSaved(PublicContribution contribution) {
    savedFullNames.add(contribution.getFullName());
  }

  public void markAsUnsaved(PublicContribution contribution) {
    savedFullNames.remove(contribution.getFullName());
  }

  public boolean isSaved(PublicContribution contribution) {
    return savedFullNames.contains(contribution.getFullName());
  }

  public Observable<Object> streamChanges() {
    return savedFullNames.changes().cast(Object.class);
  }
}
