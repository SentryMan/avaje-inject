package org.example.coffee.provider;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
class ProtoTypeNumberGetter {

  private final Provider<Long> nProv;

  ProtoTypeNumberGetter(Provider<Long> nProv) {
    this.nProv = nProv;
  }

  Long number() {
    return nProv.get();
  }
}
