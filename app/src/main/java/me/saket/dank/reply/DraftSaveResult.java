package me.saket.dank.reply;

public enum DraftSaveResult {
  SAVED_OR_UPDATED(true),
  REMOVED(false),;

  public final boolean canShowDraftSavedToast;

  DraftSaveResult(boolean canShowDraftSavedToast) {
    this.canShowDraftSavedToast = canShowDraftSavedToast;
  }
}
