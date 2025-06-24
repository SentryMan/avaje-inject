package org.example.myapp.assist.droid;

import io.avaje.inject.Assisted;
import io.avaje.inject.AssistFactory;
import javax.inject.Inject;
import javax.inject.Named;

@Named("androidfield")
@AssistFactory(DroidFactory.class)
public class DroidField implements DroidFactory.Droid {
  @Assisted int personality;
  @Assisted Model model;
  @Inject Engine wiring;

  @Named("red") @Inject Radio radio;

  public int personality() {
    return personality;
  }

  public Model model() {
    return model;
  }

  @Override
  public boolean dependenciesAreWired() {
    return wiring != null && radio != null;
  }
}
