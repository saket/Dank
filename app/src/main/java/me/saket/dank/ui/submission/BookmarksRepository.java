package me.saket.dank.ui.submission;

import net.dean.jraw.models.PublicContribution;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import timber.log.Timber;

public class BookmarksRepository {

  private final Set<String> savedFullNames = new HashSet<>();

  @Inject
  public BookmarksRepository() {
  }

  public void markAsSaved(PublicContribution contribution) {
    Timber.i("TODO: save");
    savedFullNames.add(contribution.getFullName());
  }

  public void markAsUnsaved(PublicContribution contribution) {
    Timber.i("TODO: save");
    savedFullNames.remove(contribution.getFullName());
  }

  public boolean isSaved(PublicContribution contribution) {
    return savedFullNames.contains(contribution.getFullName());
  }
}
