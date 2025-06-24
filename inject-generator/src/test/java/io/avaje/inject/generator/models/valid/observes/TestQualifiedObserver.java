package io.avaje.inject.generator.models.valid.observes;

import java.util.List;

import io.avaje.inject.events.Observes;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TestQualifiedObserver {

  void observe(@Observes @Named("list") List<String> e) {}
}
