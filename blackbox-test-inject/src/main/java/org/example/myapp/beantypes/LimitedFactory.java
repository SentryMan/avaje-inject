package org.example.myapp.beantypes;

import io.avaje.inject.Bean;
import io.avaje.inject.BeanTypes;
import io.avaje.inject.Factory;
import javax.inject.Named;

@Factory
public class LimitedFactory {

  @Bean
  @Named("factory")
  @BeanTypes(LimitedInterface.class)
  BeanTypeComponent bean() {
    return new BeanTypeComponent();
  }
}
