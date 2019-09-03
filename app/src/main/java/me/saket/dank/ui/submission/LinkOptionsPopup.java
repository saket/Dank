package me.saket.dank.ui.submission;

import android.content.Context;
import android.widget.Toast;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.urlparser.Link;
import me.saket.dank.utils.Clipboards;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.NestedOptionsPopupMenu;

public class LinkOptionsPopup extends NestedOptionsPopupMenu {

  private static final int ID_SHARE = 0;
  private static final int ID_COPY = 1;
  private static final int ID_OPEN_IN_EXTERNAL_BROWSER = 2;

  @Inject @Named("open_links_in_external_browser") Preference<Boolean> openLinksInExternalBrowserPref;

  private final Link link;

  public LinkOptionsPopup(Context c, Link link) {
    super(c);
    Dank.dependencyInjector().inject(this);

    this.link = link;
    createMenuLayout(c, menuStructure(c));
  }

  private MenuStructure menuStructure(Context c) {
    List<MenuStructure.SingleLineItem> menuItems = new ArrayList<>();
    menuItems.add(MenuStructure.SingleLineItem.create(ID_SHARE, c.getString(R.string.link_option_share), R.drawable.ic_share_20dp));
    menuItems.add(MenuStructure.SingleLineItem.create(ID_COPY, c.getString(R.string.link_option_copy), R.drawable.ic_copy_20dp));

    boolean isInternalBrowserEnabled = !openLinksInExternalBrowserPref.get();
    if (isInternalBrowserEnabled) {
      menuItems.add(MenuStructure.SingleLineItem.create(
          ID_OPEN_IN_EXTERNAL_BROWSER,
          c.getString(R.string.link_option_open_in_external_browser),
          R.drawable.ic_web_browser_20dp));
    }

    return MenuStructure.create(link.unparsedUrl(), menuItems);
  }

  @Override
  protected void handleAction(Context c, int actionId) {
    switch (actionId) {
      case ID_SHARE:
        c.startActivity(Intents.createForSharingUrl(c, link.unparsedUrl()));
        break;

      case ID_COPY:
        Clipboards.save(c, link.unparsedUrl());
        Toast.makeText(c, R.string.copy_to_clipboard_confirmation, Toast.LENGTH_SHORT).show();
        break;

      case ID_OPEN_IN_EXTERNAL_BROWSER:
        c.startActivity(Intents.createForOpeningUrl(link.unparsedUrl()));
        break;

      default:
        throw new UnsupportedOperationException();
    }
    dismiss();
  }
}
