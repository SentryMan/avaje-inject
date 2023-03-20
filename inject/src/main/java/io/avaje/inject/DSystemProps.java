package io.avaje.inject;

import java.util.Objects;
import java.util.Optional;

import io.avaje.lang.NonNullApi;

@NonNullApi
final class DSystemProps implements io.avaje.inject.spi.PropertyRequiresPlugin {

  @Override
  public Optional<String> get(String property) {
    return Optional.ofNullable(System.getProperty(property))
        .or(() -> Optional.ofNullable(System.getenv(property)));
  }

  @Override
  public void set(String property, String value) {
    Objects.requireNonNull(value);
    System.setProperty(property, value);
  }

  @Override
  public boolean contains(String property) {
    return System.getProperty(property) != null || System.getenv(property) != null;
  }

  @Override
  public boolean missing(String property) {
    return System.getProperty(property) == null && System.getenv(property) == null;
  }

  @Override
  public boolean equalTo(String property, String value) {
    return value.equals(System.getProperty(property)) || value.equals(System.getenv(property));
  }

  @Override
  public boolean notEqualTo(String property, String value) {
    return !value.equals(System.getProperty(property)) && !value.equals(System.getenv(property));
  }
}
