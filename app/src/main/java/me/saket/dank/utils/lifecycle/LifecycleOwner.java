package me.saket.dank.utils.lifecycle;

public interface LifecycleOwner<EVENT> {

  LifecycleStreams<EVENT> lifecycle();
}
