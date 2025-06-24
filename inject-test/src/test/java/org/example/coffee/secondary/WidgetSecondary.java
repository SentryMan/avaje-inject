package org.example.coffee.secondary;

import io.avaje.inject.Secondary;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Secondary
@Named("doesNotExist")
public class WidgetSecondary implements Widget {

  @Override
  public String wid() {
    return "second";
  }
}
