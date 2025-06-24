package org.example.observes;

import io.avaje.inject.events.Observes;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MyQualifiedObserver {

  boolean invoked = false;
  CustomEvent event;

  void observe(@Observes @Named("qual") CustomEvent e) {
    invoked = true;
    event = e;
  }
}
