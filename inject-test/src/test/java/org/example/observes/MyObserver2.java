package org.example.observes;

import io.avaje.inject.events.Observes;
import javax.inject.Singleton;

@Singleton
public class MyObserver2 {

  boolean invoked = false;
  CustomEvent event;

  void observe(@Observes @StrQualifier("foo") CustomEvent e) {
    invoked = true;
    event = e;
  }
}
