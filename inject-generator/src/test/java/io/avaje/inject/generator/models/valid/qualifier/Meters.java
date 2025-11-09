package io.avaje.inject.generator.models.valid.qualifier;

import io.avaje.inject.generator.models.valid.qualifier.TempQualifier.Scale;
import javax.inject.Singleton;

@Singleton
public class Meters {

  Thermometer imperial;

  Thermometer metric;

  public Meters(
      @TempQualifier(value = Scale.FAHRENHEIT, someOtherString = "far") Thermometer imperial,
      @TempQualifier(value = Scale.CELSIUS, someOtherString = "celsi") Thermometer metric) {
    this.imperial = imperial;
    this.metric = metric;
  }
}
