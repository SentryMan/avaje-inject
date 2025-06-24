package io.avaje.inject.generator.models.valid.lazy;

import io.avaje.inject.Lazy;
import io.avaje.inject.generator.models.valid.Timed;
import javax.inject.Inject;
import javax.inject.Singleton;

@Lazy
@Timed
@Singleton
public class LazyBeanAOP {

  Integer intProvider;

  @Inject
  public LazyBeanAOP(Integer intProvider) {
    this.intProvider = intProvider;
  }

  public LazyBeanAOP() {}

  void something() {}
}
