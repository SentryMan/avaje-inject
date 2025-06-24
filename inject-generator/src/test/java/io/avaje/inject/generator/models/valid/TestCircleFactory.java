package io.avaje.inject.generator.models.valid;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TestCircleFactory {

  private final String parent;

  @Inject
  public TestCircleFactory(@Named("parent") String parent) {
    this.parent = parent;
  }

  public String getParent() {
    return parent;
  }
}
