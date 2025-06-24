package org.example.myapp.lazy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

import io.avaje.inject.BeanScope;
import io.avaje.inject.Lazy;
import io.avaje.inject.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Lazy
@Singleton
@Named("single")
public class LazyBean {

  @Inject @Nullable AtomicBoolean initialized;

  @PostConstruct
  void init(BeanScope scope) {
    // note that nested test scopes will not be lazy
    if (initialized != null) initialized.set(true);
  }

  void something() {}

  public void other() {}
}
