package me.saket.dank.ui.user.messages;

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

import me.saket.dank.R;
import me.saket.dank.utils.Optional;

public interface InboxMessageThread {

  @AutoValue
  abstract class UiModel implements InboxFolderScreenUiModel {

    @Override
    public abstract long adapterId();

    public abstract Optional<String> secondPartyName();

    public abstract String subject();

    public abstract String snippet();

    public abstract String timestamp();

    @Override
    public Type type() {
      return Type.MESSAGE_THREAD;
    }

    @Override
    public abstract Message message();

    public static UiModel create(
        long adapterId,
        Optional<String> secondPartyName,
        String subject,
        String snippet,
        String timestamp,
        Message message) {
      return new AutoValue_InboxMessageThread_UiModel(adapterId, secondPartyName, subject, snippet, timestamp, message);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    private TextView secondPartyNameView;
    private TextView subjectView;
    private TextView snippetView;
    private TextView timestampView;
    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_inbox_message_thread, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      secondPartyNameView = itemView.findViewById(R.id.messagethread_second_party);
      subjectView = itemView.findViewById(R.id.messagethread_subject);
      snippetView = itemView.findViewById(R.id.messagethread_snippet);
      timestampView = itemView.findViewById(R.id.messagethread_timestamp);
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      uiModel.secondPartyName().ifPresent(name -> secondPartyNameView.setText(name));
      secondPartyNameView.setVisibility(uiModel.secondPartyName().isPresent() ? View.VISIBLE : View.GONE);
      subjectView.setText(uiModel.subject());
      snippetView.setText(uiModel.snippet());
      timestampView.setText(uiModel.timestamp());
    }
  }

  class Adapter implements InboxFolderScreenUiModel.Adapter<UiModel, ViewHolder> {
    public PublishRelay<MessageClickEvent> messageClicks = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreate(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
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
