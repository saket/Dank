package me.saket.dank.ui.user.messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Message;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankActivity;

/**
 * For composing a new message or a reply to an existing message or comment.
 */
public class ComposeReplyActivity extends DankActivity {

  private static final String KEY_RECEIVER_NAME = "receiverName";
  private static final String KEY_CONTRIBUTION_JSON = "contributionJson";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.composereply_progress) View progressView;

  public static void start(Context context, String receiverName, Message replyToMessage, Moshi moshi) {
    Intent intent = new Intent(context, ComposeReplyActivity.class);
    intent.putExtra(KEY_RECEIVER_NAME, receiverName);
    intent.putExtra(KEY_CONTRIBUTION_JSON, moshi.adapter(Message.class).toJson(replyToMessage));
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.compose_reply);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setTitle(getString(R.string.composereply_title_reply_to, getIntent().getStringExtra(KEY_RECEIVER_NAME)));
    toolbar.setNavigationOnClickListener(o -> onBackPressed());
  }

  @OnClick(R.id.composereply_send)
  void onClickSend() {
    progressView.setVisibility(View.VISIBLE);
  }
}
