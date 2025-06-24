package io.avaje.inject.generator.models.valid.observes;

import io.avaje.inject.events.Observes;
import javax.inject.Singleton;

@Singleton
public class TestObserver {

  void observe(@Observes CustomEvent e) {}
}
