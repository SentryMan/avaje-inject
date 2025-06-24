package io.avaje.inject.generator.models.valid;

import java.lang.annotation.Annotation;

import io.avaje.inject.External;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LangInject {

  @Inject @External Annotation classLoadingMXBean;
}
