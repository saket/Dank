package me.saket.dank.ui.user.messages;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.Arrays;

public class InboxPagerAdapter extends FragmentStatePagerAdapter {

  private Resources resources;
  private InboxFolderFragment activeFragment;

  public InboxPagerAdapter(Resources resources, FragmentManager manager) {
    super(manager);
    this.resources = resources;
  }

  public int getPosition(InboxFolder folder) {
    return Arrays.binarySearch(InboxFolder.getALL(), folder);
  }

  @Override
  public Fragment getItem(int position) {
    return InboxFolderFragment.create(InboxFolder.getALL()[position]);
  }

  @Override
  public int getCount() {
    return InboxFolder.getALL().length;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return resources.getString(InboxFolder.getALL()[position].titleRes());
  }

  @Override
  public void setPrimaryItem(ViewGroup container, int position, Object object) {
    super.setPrimaryItem(container, position, object);
    activeFragment = (InboxFolderFragment) object;
  }

  public InboxFolderFragment getActiveFragment() {
    return activeFragment;
  }
}
