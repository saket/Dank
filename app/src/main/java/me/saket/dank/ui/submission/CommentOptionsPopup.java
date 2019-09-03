package me.saket.dank.ui.submission;

import android.content.Context;
import android.widget.Toast;

import net.dean.jraw.models.Comment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.utils.Clipboards;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.NestedOptionsPopupMenu;
import me.saket.dank.utils.markdown.Markdown;
import okhttp3.HttpUrl;

public class CommentOptionsPopup extends NestedOptionsPopupMenu {

  private static final int ID_SHOW_USER_PROFILE = 0;
  private static final int ID_SAVE = 1_1;
  private static final int ID_UNSAVE = 1_2;
  private static final int ID_SHARE_PERMALINK = 2;
  private static final int ID_COPY_PERMALINK = 3;

  @Inject Lazy<Markdown> markdown;
  @Inject Lazy<BookmarksRepository> bookmarksRepository;

  private final Comment comment;

  public CommentOptionsPopup(Context c, Comment comment) {
    super(c);
    this.comment = comment;

    Dank.dependencyInjector().inject(this);
    createMenuLayout(c, menuStructure(c));
  }

  private MenuStructure menuStructure(Context c) {
    //noinspection AccessStaticViaInstance
    String commentBody = markdown.get().stripMarkdown(comment);

    List<MenuStructure.SingleLineItem> primaryItems = new ArrayList<>(3);
    primaryItems.add(MenuStructure.SingleLineItem.create(
        ID_SHOW_USER_PROFILE,
        c.getString(R.string.user_name_u_prefix, comment.getAuthor()),
        R.drawable.ic_user_profile_20dp
    ));
    if (bookmarksRepository.get().isSaved(comment)) {
      primaryItems.add(MenuStructure.SingleLineItem.create(
          ID_UNSAVE,
          c.getString(R.string.submission_comment_option_unsave),
          R.drawable.ic_unsave_24dp
      ));
    } else {
      primaryItems.add(MenuStructure.SingleLineItem.create(
          ID_SAVE,
          c.getString(R.string.submission_comment_option_save),
          R.drawable.ic_save_20dp
      ));
    }
    primaryItems.add(MenuStructure.SingleLineItem.create(
        ID_SHARE_PERMALINK,
        c.getString(R.string.submission_comment_option_share_link),
        R.drawable.ic_share_20dp
    ));
    primaryItems.add(MenuStructure.SingleLineItem.create(
        ID_COPY_PERMALINK,
        c.getString(R.string.submission_comment_option_copy_link),
        R.drawable.ic_copy_20dp
    ));
    return MenuStructure.create(commentBody, primaryItems);
  }

  @Override
  protected void handleAction(Context c, int actionId) {
    //noinspection ConstantConditions
    String permalinkWithContext = HttpUrl.parse("https://reddit.com" + comment.getPermalink())
        .newBuilder()
        .addQueryParameter(Reddit.CONTEXT_QUERY_PARAM, String.valueOf(Reddit.COMMENT_DEFAULT_CONTEXT_COUNT))
        .build()
        .toString();

    switch (actionId) {
      case ID_SHOW_USER_PROFILE:
        Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show();
        break;

      case ID_UNSAVE:
        bookmarksRepository.get().markAsUnsaved(comment);
        break;

      case ID_SAVE:
        bookmarksRepository.get().markAsSaved(comment);
        break;

      case ID_SHARE_PERMALINK:
        c.startActivity(Intents.createForSharingUrl(c, permalinkWithContext));
        break;

      case ID_COPY_PERMALINK:
        Clipboards.save(c, permalinkWithContext);
        Toast.makeText(c, R.string.copy_to_clipboard_confirmation, Toast.LENGTH_SHORT).show();
        break;

      default:
        throw new UnsupportedOperationException("actionId: " + actionId);
    }
    dismiss();
  }
}
