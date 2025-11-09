package org.example.myapp.config;

import io.avaje.inject.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class LifeFour {

  public String _state;

  @PostConstruct
  void post(@Named("foo") LifeOne one, LifeTwo two) {
    _state = "post|"
      + (one != null ? "one|" : "")
      + (two != null ? "two" : "");
  }
}
