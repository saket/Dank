package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setPaddingBottom;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import me.saket.dank.R;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class PrivateMessageThreadActivity extends DankPullCollapsibleActivity {

  private static final String KEY_MESSAGE_ID = "messageId";
  private static final String KEY_THREAD_SECOND_PARTY_NAME = "threadSecondPartyName";

  @BindView(R.id.privatemessagethread_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.privatemessagethread_subject) TextView threadSubjectView;
  @BindView(R.id.privatemessagethread_message_list) RecyclerView messageRecyclerView;
  @BindView(R.id.privatemessagethread_reply) FloatingActionButton replyButton;

  @Inject UrlRouter urlRouter;

  private ThreadedMessagesAdapter messagesAdapter;

  public static void start(Context context, String messageId, String threadSecondPartyName, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, PrivateMessageThreadActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    intent.putExtra(KEY_MESSAGE_ID, messageId);
    intent.putExtra(KEY_THREAD_SECOND_PARTY_NAME, threadSecondPartyName);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_private_message_thread);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setTitle(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME));

    setupContentExpandablePage(contentPage);
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    executeOnMeasure(replyButton, () -> {
      MarginLayoutParams fabMarginLayoutParams = (MarginLayoutParams) replyButton.getLayoutParams();
      int spaceForFab = replyButton.getHeight() + fabMarginLayoutParams.topMargin + fabMarginLayoutParams.bottomMargin;
      setPaddingBottom(messageRecyclerView, spaceForFab);
    });

    unsubscribeOnDestroy(Single.timer(200, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(o -> replyButton.show())
    );

    DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
    linkMovementMethod.setOnLinkClickListener((textView, url) -> {
      Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();
      Link parsedLink = UrlParser.parse(url);

      if (parsedLink instanceof RedditUserLink) {
        urlRouter.forLink(((RedditUserLink) parsedLink))
            .expandFrom(clickedUrlCoordinates)
            .open(textView);

      } else {
        urlRouter.forLink(parsedLink)
            .expandFrom(clickedUrlCoordinates)
            .open(this);
      }
      return true;
    });

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messageRecyclerView.setLayoutManager(layoutManager);

    // TODO: Empty state
    messagesAdapter = new ThreadedMessagesAdapter(linkMovementMethod);
    messageRecyclerView.setAdapter(messagesAdapter);

    unsubscribeOnDestroy(Dank.inbox().message(getIntent().getStringExtra(KEY_MESSAGE_ID), InboxFolder.PRIVATE_MESSAGES)
        .compose(applySchedulers())
        .doOnNext(message -> threadSubjectView.setText(message.getSubject()))
        .map(parentMessage -> {
          List<Message> messageReplies = JrawUtils.messageReplies(parentMessage);
          List<Message> messages = new ArrayList<>(messageReplies.size() + 1);
          messages.add(parentMessage);
          messages.addAll(messageReplies);
          return messages;
        })
        .doAfterNext(o -> {
          // Without this line, the list does not start at the bottom.
          // Seems to be a bug with RV when its height is set to wrap_content.
          messageRecyclerView.post(() -> messageRecyclerView.smoothScrollToPosition(messagesAdapter.getItemCount()));
        })
        .subscribe(messagesAdapter)
    );

    contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) ->
        touchLiesOn(messageRecyclerView, downX, downY) && messageRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1)
    );
  }

  @OnClick(R.id.privatemessagethread_reply)
  void onClickReply() {
    List<Message> messages = messagesAdapter.getData();
    Message latestMessage = messages.get(messages.size() - 1);
    ComposeStartOptions startOptions = ComposeStartOptions.builder()
        .secondPartyName(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME))
        .parentContributionFullName(latestMessage.getFullName())
        .build();
    ComposeReplyActivity.start(this, startOptions);
  }
}
