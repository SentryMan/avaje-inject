package org.example.coffee.qualifier;

import javax.inject.Singleton;

// no qualifier on this one
@Singleton
public class NoNameStore implements SomeStore {

  @Override
  public String store() {
    return "noName";
  }

}
