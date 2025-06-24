package io.avaje.inject.generator.models.valid.lazy;

import io.avaje.inject.Lazy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Lazy
@Singleton
public class LazyBean {

  Provider<Integer> intProvider;

  @Inject
  public LazyBean(Provider<Integer> intProvider) {
    this.intProvider = intProvider;
  }

  public LazyBean() {}

  void something() {}
}
