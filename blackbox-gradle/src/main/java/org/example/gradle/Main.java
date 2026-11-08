package org.example.gradle;

import io.avaje.inject.BeanScope;
import io.avaje.inject.MainClass;

@MainClass
public class Main {

  public static void main(String[] args) {
    try (BeanScope beanScope = BeanScope.builder().build()) {
      System.out.println(beanScope.get(HelloService.class).hello());
    }
  }
}
