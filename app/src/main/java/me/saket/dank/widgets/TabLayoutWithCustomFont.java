package me.saket.dank.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import me.saket.dank.R;

/**
 * Because TabLayout does not let us style the title View.
 */
public class TabLayoutWithCustomFont extends TabLayout {

    public TabLayoutWithCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        tab.setCustomView(R.layout.view_viewpager_tab);
        super.addTab(tab, position, setSelected);
    }

}
