package io.avaje.inject.generator.models.valid.duplicate;

import javax.inject.Singleton;

@Singleton
public class SameType {
  @Singleton
  public static class Inner {}
}
