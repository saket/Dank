package me.saket.dank.widgets.swipe;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SwipeActions {

  public abstract SwipeActionsHolder startActions();

  public abstract SwipeActionsHolder endActions();

  public static Builder builder() {
    return new AutoValue_SwipeActions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder startActions(SwipeActionsHolder startActionsHolder);

    public abstract Builder endActions(SwipeActionsHolder endActionsHolder);

    public abstract SwipeActions build();
  }

}
