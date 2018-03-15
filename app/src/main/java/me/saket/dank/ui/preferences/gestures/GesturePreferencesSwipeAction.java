package me.saket.dank.ui.preferences.gestures;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import java.util.List;

import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.swipe.SwipeAction;

public interface GesturePreferencesSwipeAction {

  @AutoValue
  abstract class UiModel implements GesturePreferenceUiModel {
    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.SWIPE_ACTION;
    }

    public abstract SwipeAction swipeAction();

    public abstract Drawable icon();

    public static UiModel create(SwipeAction swipeAction, Drawable icon) {
      return new AutoValue_GesturePreferencesSwipeAction_UiModel(swipeAction.labelRes(), swipeAction, icon);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private TextView labelView;

    public ViewHolder(View itemView) {
      super(itemView);
      labelView = itemView.findViewById(R.id.item_gesturepreferences_swipeaction_label);
    }

    public void render(UiModel uiModel) {
      labelView.setText(uiModel.swipeAction().labelRes());
      Views.setCompoundDrawableStart(labelView, uiModel.icon());
    }
  }

  class Adapter implements GesturePreferenceUiModel.ChildAdapter<UiModel, ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_gesture_preference_swipe_action, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.render(uiModel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
