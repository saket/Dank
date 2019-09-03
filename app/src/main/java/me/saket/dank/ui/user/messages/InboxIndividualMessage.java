package me.saket.dank.ui.user.messages;

import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Message;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.utils.DankLinkMovementMethod;

public interface InboxIndividualMessage {

  @AutoValue
  abstract class UiModel implements InboxFolderScreenUiModel {

    @Override
    public abstract long adapterId();

    public abstract String title();

    public abstract String byline();

    public abstract String senderInformation();

    public abstract SpannableWithTextEquality body();

    @Override
    public Type type() {
      return Type.INDIVIDUAL_MESSAGE;
    }

    @Override
    public abstract Message message();

    public static UiModel create(
        long adapterId,
        String title,
        String byline,
        String senderInformation,
        CharSequence body,
        Message message)
    {
      return new AutoValue_InboxIndividualMessage_UiModel(
          adapterId,
          title,
          byline,
          senderInformation,
          SpannableWithTextEquality.wrap(body),
          message);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private TextView titleView;
    private TextView bylineView;
    private TextView senderInformationView;
    private TextView messageBodyView;
    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_inbox_individual_message, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.individualmessage_reply_post_title);
      bylineView = itemView.findViewById(R.id.individualmessage_reply_byline);
      senderInformationView = itemView.findViewById(R.id.individualmessage_sender_information);
      messageBodyView = itemView.findViewById(R.id.individualmessage_reply_body);
    }

    public void forwardTouchEventsToBackground(BetterLinkMovementMethod linkMovementMethod) {
      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      messageBodyView.setOnTouchListener((__, event) -> {
        boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(messageBodyView, ((Spannable) messageBodyView.getText()), event);
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
    }

    public void setBodyLinkMovementMethod(DankLinkMovementMethod movementMethod) {
      messageBodyView.setMovementMethod(movementMethod);
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());
      senderInformationView.setText(uiModel.senderInformation());
      messageBodyView.setText(uiModel.body());
    }
  }

  class Adapter implements InboxFolderScreenUiModel.Adapter<UiModel, ViewHolder> {
    private Lazy<DankLinkMovementMethod> linkMovementMethod;
    PublishRelay<MessageClickEvent> messageClicks = PublishRelay.create();

    @Inject
    public Adapter(Lazy<DankLinkMovementMethod> linkMovementMethod) {
      this.linkMovementMethod = linkMovementMethod;
    }

    @Override
    public ViewHolder onCreate(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.setBodyLinkMovementMethod(linkMovementMethod.get());
      holder.forwardTouchEventsToBackground(linkMovementMethod.get());
      holder.itemView.setOnClickListener(o ->
          messageClicks.accept(MessageClickEvent.create(holder.uiModel.message(), holder.itemView))
      );
      return holder;
    }

    @Override
    public void onBind(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.render();
    }

    @Override
    public void onBind(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
