package io.avaje.inject.generator.models.valid.observes;

import io.avaje.inject.events.ObservesAsync;
import javax.inject.Singleton;

@Singleton
public class TestObserverInjection {

  void observe(@ObservesAsync String e, TestObserver observer) {}
}
