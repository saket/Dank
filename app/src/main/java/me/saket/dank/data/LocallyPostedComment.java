package me.saket.dank.data;

@Deprecated
public class LocallyPostedComment {

//  private final PendingSyncReply pendingSyncReply;
//
//  public static LocallyPostedComment create(PendingSyncReply pendingSyncReply) {
//    return new LocallyPostedComment(pendingSyncReply);
//  }
//
//  private LocallyPostedComment(PendingSyncReply pendingSyncReply) {
//    super(null);
//    this.pendingSyncReply = pendingSyncReply;
//  }
//
//  @Nullable
//  @Override
//  public String getFullName() {
//    return pendingSyncReply.postedFullName();
//  }
//
//  @Override
//  public String getAuthor() {
//    return pendingSyncReply.author();
//  }
//
//  @Override
//  public String getBody() {
//    return pendingSyncReply.body();
//  }
//
//  @Override
//  public String getSubmissionId() {
//    String prefix = FullNameType.SUBMISSION.prefix();
//    return pendingSyncReply.parentThreadFullName().substring(prefix.length());
//  }
//
//  public String getPostingStatusIndependentId() {
//    return pendingSyncReply.parentContributionFullName() + "_reply_" + pendingSyncReply.createdTimeMillis();
//  }
//
//  public PendingSyncReply pendingSyncReply() {
//    return pendingSyncReply;
//  }
//
//  @Override
//  public boolean equals(Object o) {
//    if (this == o) {
//      return true;
//    }
//    if (!(o instanceof LocallyPostedComment)) {
//      return false;
//    }
//    LocallyPostedComment that = (LocallyPostedComment) o;
//    return pendingSyncReply.equals(that.pendingSyncReply);
//  }
//
//  @Override
//  public int hashCode() {
//    return pendingSyncReply.hashCode();
//  }
}
