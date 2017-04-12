package me.saket.dank.ui.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class MessagesActivity extends DankPullCollapsibleActivity {

    @BindView(R.id.messages_root) IndependentExpandablePageLayout contentPage;
    @BindView(R.id.messages_tablayout) TabLayout tabLayout;
    @BindView(R.id.messages_viewpager) ViewPager viewPager;

    /**
     * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
     */
    public static void start(Context context, @Nullable Rect expandFromShape) {
        Intent intent = new Intent(context, MessagesActivity.class);
        intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        ButterKnife.bind(this);
        findAndSetupToolbar();

        setupContentExpandablePage(contentPage);
        expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
    }

}
