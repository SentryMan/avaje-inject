package org.example.myapp.pkg_private;

import javax.inject.Singleton;

@Singleton
class AdderImpl implements Adder {

  @Override
  public int add(int a, int b) {
    return a + b;
  }
}
